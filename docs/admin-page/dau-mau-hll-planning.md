# DAU/MAU — Redis HyperLogLog & Micro-batching với ShedLock

> **Status:** Final plan | **Last updated:** 2026-07-10 | **Owner:** A

---

## 1. Tổng quan kiến trúc

```
User Action (AI chat / Save diagram)
    │
    ▼
CoreActivityTracker.trackActivity(userId)
    │  PFADD active_users:YYYY-MM-DD {userId}
    ▼
┌─────────────────────────────────────────────┐
│                  Redis                       │
│  HyperLogLog key: active_users:2026-07-10   │
│  TTL: 32 ngày (set bởi sync job)            │
│  RAM: ~12KB / key (1M users)                │
└─────────────────────────────────────────────┘
    ▲                                      │
    │ PFCOUNT                              │ PFMERGE 30 keys (sync job)
    ▼                                      ▼
┌──────────────┐           ┌──────────────────────────────┐
│ /overview    │           │  syncDauMau (@Scheduled)     │
│ (real-time)  │           │  chạy 0 */15 * * * *        │
└──────────────┘           │  + @SchedulerLock            │
                           │  PFCOUNT → Upsert DB         │
                           └──────────┬───────────────────┘
                                      │
                                      ▼
                     ┌─────────────────────────────┐
                     │  PostgreSQL                  │
                     │  daily_saas_metrics          │
                     │  UNIQUE(snapshot_date)       │
                     └─────────────────────────────┘
```

---

## 2. Yêu cầu Business (BR) — Ánh xạ giải pháp

| BR | Mô tả | Giải pháp |
|----|-------|-----------|
| BR-01 | Active = có hành động tạo giá trị (AI gen, save diagram/project), không tính login/surf | `CoreActivityTracker.trackActivity(userId)` gọi từ DiagramChatServiceImpl + ProjectController + SheetController |
| BR-02 | Dashboard hiển thị DAU real-time ≤ 100ms | `PFCOUNT` Redis trực tiếp — ~0.1ms, không query DB |
| BR-03 | RPO ≤ 15 phút nếu toàn bộ cụm sập | Micro-batching 15 phút ghi xuống PostgreSQL; PG là source of truth cuối cùng |

---

## 3. Yêu cầu Chức năng (FR) — Thiết kế chi tiết

### FR-01: Ghi nhận hành động — `CoreActivityTracker`

```java
@Component
@RequiredArgsConstructor
public class CoreActivityTracker {
    private final StringRedisTemplate stringRedisTemplate;

    public void trackActivity(String userId) {
        String key = "active_users:" + LocalDate.now(ZoneId.of("UTC"));
        stringRedisTemplate.opsForHyperLogLog().add(key, userId);
    }
}
```

- **Chỉ 1 lệnh PFADD** — không EXPIRE ở đây (tránh thừa I/O)
- **Không interface** (quá đơn giản, 1 method) — nếu sau mở rộng thì refactor

### FR-02: API Overview đọc DAU real-time

Trong `DashboardServiceImpl.getOverview()`:
- **DAU** = `PFCOUNT active_users:today`
- **MAU** = đọc từ `daily_saas_metrics` (bản ghi gần nhất — đã sync bởi job)

### FR-03: Micro-batching Sync Job — `DailySaasMetricSyncJob`

```java
@Scheduled(cron = "0 */15 * * * *")
@SchedulerLock(name = "dailySaasMetricSync", lockAtMostFor = "14m", lockAtLeastFor = "2m")
@Transactional
public void syncDauMau() {
    LocalDate today = LocalDate.now(ZoneId.of("UTC"));
    String todayKey = "active_users:" + today;

    // 1. DAU
    Long dau = stringRedisTemplate.opsForHyperLogLog().size(todayKey);

    // 2. Refresh TTL — fix Memory Leak, self-healing
    stringRedisTemplate.expire(todayKey, Duration.ofDays(32));

    // 3. MAU = PFMERGE 30 keys
    String destKey = "active_users:mau:" + today;
    List<String> keys = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
        keys.add("active_users:" + today.minusDays(i));
    }
    stringRedisTemplate.opsForHyperLogLog().union(destKey, keys.toArray(new String[0]));
    Long mau = stringRedisTemplate.opsForHyperLogLog().size(destKey);
    stringRedisTemplate.delete(destKey);

    // 4. Upsert — unique constraint DB-level chống duplicate
    repository.findBySnapshotDate(today).ifPresentOrElse(
        metric -> { metric.setDau(dau.intValue()); metric.setMau(mau.intValue()); repository.save(metric); },
        () -> repository.save(DailySaasMetric.builder().snapshotDate(today).dau(dau.intValue()).mau(mau.intValue()).build())
    );
}
```

### FR-04: ShedLock — Distributed Lock

- **Provider:** `JdbcTemplateLockProvider` (lưu lock trong PostgreSQL)
- **Lock table:** `shedlock` (cần tạo bằng migration)
- **`lockAtMostFor = "14m"`** (< 15m cycle, chống deadlock)
- **`lockAtLeastFor = "2m"`** (chống clock-skew)

---

## 4. Yêu cầu Phi chức năng (NFR)

| NFR | Yêu cầu | Hiện trạng / Cách đáp ứng |
|-----|---------|--------------------------|
| NFR-01 | 1M users ≤ 15KB RAM | HyperLogLog bản chất ~12KB. 32 keys × 12KB = 384KB — hoàn toàn an toàn |
| NFR-02 | Redis AOF appendfsync everysec | Cấu hình trong `redis.conf` của Docker |
| NFR-03 | ShedLock: lockAtMostFor < cycle, lockAtLeastFor chống clock-skew | Đã config trong `@SchedulerLock` (14m / 2m) |

---

## 5. Các lỗ hổng đã fix qua review

| # | Lỗ hổng | Fix |
|---|---------|-----|
| 1 | TTL 36h → key chết sau 1.5 ngày, MAI PFMERGE ra 0 | TTL = **32 ngày** |
| 2 | `LocalDate.now()` lấy timezone JVM, lệch UTC | **`LocalDate.now(ZoneId.of("UTC"))`** |
| 3 | Thiếu bảng `shedlock` → crash ngay khi start | Thêm **migration V003__shedlock.sql** |
| 4 | PFADD + EXPIRE = 2 network calls mỗi lần track | `trackActivity()` **chỉ PFADD**, EXPIRE chuyển sang sync job |
| 5 | Cronjob 23:59 set TTL dễ miss → Memory Leak | **Sync job 15 phút/lần** tự set TTL (self-healing) |

---

## 6. File sinh ra / sửa đổi

### Mới tạo

| File | Mô tả |
|------|-------|
| `entity/DailySaasMetric.java` | Entity mapping `daily_saas_metrics` |
| `repository/DailySaasMetricRepository.java` | `findBySnapshotDate` |
| `config/scheduler/ShedLockConfig.java` | `LockProvider` bean |
| `service/CoreActivityTracker.java` | Component PFADD wrapper |
| `service/Impl/DailySaasMetricSyncJob.java` | `@Scheduled` + `@SchedulerLock` sync job |
| `docs/migrations/V002__daily_saas_metrics.sql` | `daily_saas_metrics` table + unique index |
| `docs/migrations/V003__shedlock.sql` | `shedlock` table |

### Sửa đổi

| File | Thay đổi |
|------|----------|
| `pom.xml` | Thêm `shedlock-spring` + `shedlock-provider-jdbc-template` |
| `BeApplication.java` | Thêm `@EnableSchedulerLock(defaultLockAtMostFor = "14m")` |
| `service/Impl/DashboardServiceImpl.java` | `getOverview()`: DAU từ PFCOUNT, MAU từ DB |
| `service/Impl/DiagramChatServiceImpl.java` | Inject `CoreActivityTracker`, gọi `trackActivity()` |
| `controller/ProjectController.java` | Inject `CoreActivityTracker`, gọi trên POST + PATCH |
| `controller/SheetController.java` | Inject `CoreActivityTracker`, gọi trên POST + PATCH |

---

## 7. DB Migration

### V002__daily_saas_metrics.sql

```sql
CREATE TABLE daily_saas_metrics (
    id UUID PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    dau INTEGER NOT NULL,
    mau INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_daily_saas_metrics_date ON daily_saas_metrics(snapshot_date);
```

### V003__shedlock.sql

```sql
CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
```

---

## 8. Câu hỏi mở (đã chốt)

| Câu hỏi | Quyết định |
|---------|-----------|
| `trackActivity` có interface không? | **Không** — 1 method, quá đơn giản |
| EXPIRE đặt ở đâu? | **Sync job 15 phút** — self-healing, không thừa I/O |
| TTL bao nhiêu? | **32 ngày** — đủ cho PFMERGE 30 ngày |
| Tính MAU thế nào? | **PFMERGE 30 keys** trong sync job |
| Lỗi spam AI có tính active không? | **Có** — user đang tương tác, vẫn là DAU |
| Save endpoints nào? | **POST + PATCH** cả ProjectController và SheetController |
