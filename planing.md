# planing.md — Đối chiếu IMPLEMENTATION_PLAN với code thật

> **Mục đích:** ghi lại kết quả **quét dự án** để kiểm tra các giả định trong `IMPLEMENTATION_PLAN.md`
> (baseline `6a30e89`) so với **code thật trên nhánh `feat/admin-charts` (HEAD hiện tại)**.
> **Ngày quét:** 2026-07-21. **Kết luận nhanh:** cấu trúc plan tốt, nhưng **baseline đã cũ** — vài giả
> định không còn đúng, có bug live chưa nằm trong plan, và có nguy cơ trùng lặp cơ chế/field.

Quan hệ tài liệu hiện có (3 file, cùng hướng, khác tầng):

| File | Tầng |
|---|---|
| `SUBSCRIPTION_UPGRADE_PRORATION.md` | Thiết kế / business rules (WHAT) |
| `planning-contract.md` | Chiến lược + điểm chốt Owner/FE (WHO/decisions) |
| `IMPLEMENTATION_PLAN.md` | Task breakdown chi tiết (HOW, từng file) |
| `planing.md` (file này) | Nhật ký đối chiếu plan ↔ code thật |

---

## 1. Phát hiện chính (có bằng chứng)

### F1 — Chặng 0 coi như đã xong: KHÔNG còn compile blocker
- **Plan nói:** Task 0.1 phải fix `DiagramChatServiceImpl` interface mismatch trước mọi thứ.
- **Thực tế:** chạy `./mvnw.cmd compile` → **BUILD SUCCESS**. Code hiện tại compile sạch; blocker đã được giải quyết trên nhánh này.
- **Hệ quả:** Task 0.1 vô hiệu. Baseline `6a30e89` của plan đã lạc hậu so với HEAD.

### F2 — Plan chưa tính tới thay đổi Quota vừa làm (chưa commit)
- **Đã thay đổi trong working tree:**
  - `service/Impl/QuotaServiceImpl.java`: bỏ hằng `NEVER_RESET` (mốc `9999-12-31`); gói tier thấp nhất (Free, không sub) → `resetAt = now + periodDays` (30 ngày lăn). Đóng lỗi `timestamp out of range` (`LocalDateTime.MAX`).
  - `initializer/DataInitializer.java`: thêm `backfillQuotaResetAt()` — khởi động tự vá row `user_quota.reset_at >= 9000-01-01`.
  - `test/.../QuotaServiceImplTest.java`: cập nhật theo (10/10 pass).
- **Xung đột tiềm ẩn với plan:**
  - Task 1.3 định thêm `quotaPeriodStart/quotaPeriodEnd/nominalAiLimit/effectiveAiLimit/planId` vào `UserQuota` **và giữ nguyên `aiLimit/resetAt`**.
  - Task 1.17 `QuotaPeriodService` sẽ dựng lại đúng cơ chế period mà `resetAt` đang đảm nhiệm → **2 cơ chế period song song** (`resetAt` cũ vs `quotaPeriodStart/End` mới) → dễ lệch/nhân đôi logic.
  - **Cần chốt:** migrate `resetAt` → `quotaPeriodEnd`, hay giữ cả hai và quy định rõ field nào là chuẩn.

### F3 — Vài field mới TRÙNG field đã có (rủi ro "2 nguồn sự thật")
- Task 1.1 thêm `Plan.billingCycle` + `Plan.quotaPeriodDays`.
- **Nhưng `Plan.java` ĐÃ CÓ:** `yearlyBilling` (boolean), `yearlyDiscount`, `durationDays` (Integer) — trong đó `durationDays` đang được `PaymentServiceImpl` dùng để tính `endDate` (`startDate.plusDays(durationDays)`).
- → `billingCycle` / `quotaPeriodDays` / `durationDays` / `yearlyBilling` cùng nói về "chu kỳ / độ dài kỳ". **Cần quyết field nào là chuẩn**, tránh 4 field mâu thuẫn.
- `Plan.tierOrder`: **đúng là chưa có** → thêm hợp lý.

### F4 — Bug đang LIVE nhưng KHÔNG có trong plan
- `service/scheduleJobs/Impl/SaasMetricSyncServiceImpl.java:54` — `.map(s -> s.getPlan().getPrice())` đọc `Plan` lazy proxy ngoài session (method `syncMetrics()` không `@Transactional`) → `LazyInitializationException`, job daily crash.
- Plan có Task 3.2 sửa `DashboardServiceImpl` MRR nhưng **không nhắc `SaasMetricSyncServiceImpl`** (không có trong File Manifest). → **Gap**. Bug này nên đưa vào Chặng 0.

### F5 — Hành vi mua hiện tại khác thiết kế
- `PaymentServiceImpl.processWebhook()` + `getPaymentStatus()`: logic cấp subscription **bị nhân đôi**, không idempotent → rủi ro **double-grant** (webhook + polling cùng cấp).
- Hiện tại mua gói mới "nối kỳ" (`startDate = currentSub.endDate`), **không** chặn same-plan / downgrade. Plan Chặng 2 đổi hành vi này (có flag) — thay đổi lớn, cần lưu ý khi cutover.

---

## 2. Giả định của plan đã kiểm — ĐÚNG ✅

| Giả định trong plan | Bằng chứng | Kết quả |
|---|---|---|
| `PlanFeatureKey` gồm 5 key (AI_QUERIES, MAX_PROJECTS, MAX_DIAGRAMS, EXPORT_PDF, MAX_COLLABORATORS) | `enums/PlanFeatureKey.java` | ✅ Đúng |
| `PaymentStatus` thiếu `REQUIRES_REVIEW` | `enums/PaymentStatus.java` = PENDING/PAID/CANCELLED | ✅ Cần thêm (đúng) |
| `SubscriptionStatus` thiếu `REPLACED` | `enums/SubscriptionStatus.java` = ACTIVE/EXPIRED/CANCELLED | ✅ Cần thêm (đúng) |
| Có `V001` để `V002` nối tiếp | `docs/migrations/V001__plan_price_bigdecimal.sql` | ✅ Đúng |
| `ProjectRepository` theo pattern `isDeleted`/`isDraft`, thêm `...AndLifecycleStatus` khả thi | `repository/ProjectRepository.java` | ✅ Đúng convention |
| `UserQuota` chưa có nominal/effective/period/planId | `entity/UserQuota.java` | ✅ Đúng (đang thiếu) |
| `Subscription` chưa có snapshot giá | `entity/Subscription.java` | ✅ Đúng (đang thiếu) |
| Seed 4 plan UUID cố định (Free `1111…` → Pro `4444…`) map tier 0→3 | `initializer/DataInitializer.java` | ✅ Khớp default PLAN-01 |

---

## 3. Rủi ro quy mô (scope)

- **57 task, 27 file mới, 30+ file sửa**, thêm entity `EntitlementGracePeriod`, **6 scheduled job**, `ComplianceGuard` chèn vào **6 service** (Project/Sheet/Workspace/Version/Chat/Socket), archive + purge cleanup graph.
- Đây là khối lượng **nhiều tuần**. Riêng **Chặng 4/5** (reconciliation / archive / purge) ~20 task, đụng cả **Socket.IO** và **chat** → rất nặng cho timeline đồ án.

---

## 4. Khuyến nghị

1. **Cập nhật baseline** IMPLEMENTATION_PLAN: `6a30e89` → HEAD hiện tại.
   - Bỏ **Task 0.1** (đã xong, compile xanh).
   - **Thêm** vào Chặng 0: fix `SaasMetricSyncServiceImpl` LazyInit (F4).
   - Ghi nhận thay đổi Quota (F2) làm nền cho Task 1.3 / 1.17.
2. **Chốt cơ chế quota (F2):** dùng `resetAt` hiện có làm chuẩn, hay chuyển sang `quotaPeriodStart/End`? Tránh 2 cơ chế song song.
3. **Chốt field chu kỳ (F3):** thống nhất `durationDays` vs `quotaPeriodDays` vs `billingCycle` vs `yearlyBilling`.
4. **Cắt scope thực tế:** làm chắc **Chặng 0 → 1 → 2** (mua / upgrade / quota) trước; **Chặng 4/5** (archive/purge) tách hẳn, chỉ làm nếu còn thời gian.
5. **Gộp tài liệu:** để `IMPLEMENTATION_PLAN.md` là nguồn thi công chính; `planning-contract.md` chỉ giữ phần "cần chốt Owner/FE"; tránh maintain 3–4 file lệch nhau.

---

## 5. Việc còn treo (từ các bước trước)

- [ ] **A1** `reset_at` overflow — đã fix code + backfill; **chưa commit/push**, chưa restart app.
- [ ] **A2** `SaasMetricSyncServiceImpl` LazyInit — **chưa fix** (F4).
- [ ] **A3** double-grant thanh toán — **chưa fix** (F5), bản đầy đủ nằm ở Chặng 2A/2B của plan.

---

## 6. Các phụ thuộc NGUY CƠ BỊ NỔ (blast-radius map)

> Đây là các điểm ghép nối mong manh trong code hiện tại. Đụng vào phần subscription/quota mà không
> phòng thì rất dễ kéo theo lỗi runtime hoặc hỏng startup. Cột **Cách phòng** là hướng xử lý.

| # | Điểm phụ thuộc | Vì sao dễ nổ (bằng chứng) | Mức | Cách phòng |
|---|---|---|---|---|
| **R1** | `Subscription.plan` là `@ManyToOne(fetch = LAZY)` | `entity/Subscription.java:24-26`. Đọc `subscription.getPlan().getXxx()` ngoài transaction → `LazyInitializationException`. **Đã nổ thật** tại `SaasMetricSyncServiceImpl.java:54` | 🔴 Cao | Mọi nơi đọc `getPlan()` phải `@Transactional` **hoặc** `JOIN FETCH`. Đích lâu dài: đọc **snapshot** (`billingPriceSnapshot`) để metrics/không bao giờ chạm `Plan` live |
| **R2** | 2 cơ chế "kỳ quota" song song: `resetAt` (đang dùng) vs `quotaPeriodStart/End` (plan Task 1.3/1.17) | `entity/UserQuota.java:54` + `QuotaServiceImpl.syncQuotaToCurrentPlan()`. Nếu cả hai cùng ghi `user_quota` sẽ "đánh nhau", reset lệch | 🔴 Cao | **Chốt 1 cơ chế.** Khuyến nghị giữ `resetAt` làm chuẩn; `QuotaPeriodService` (nếu có) ghi vào `resetAt`, KHÔNG tạo field period song song |
| **R3** | Cấp subscription nhân đôi: webhook + polling | `PaymentServiceImpl.processWebhook()` (~176) và `getPaymentStatus()` (~227) đều tự `new Subscription` + `resetOnPlanChange`, không idempotent | 🔴 Cao | Rút về **1 method** `activate(payment)` khóa bằng conditional `UPDATE ... WHERE status = PENDING` (đếm rows), cả 2 caller gọi chung |
| **R4** | `User.currentSubscription` đọc/ghi rải rác | `PaymentServiceImpl.java:167` đọc, `:186` ghi. Nếu Chặng 2B cho base user **không có** Subscription → mọi nơi đọc `currentSubscription` phải chịu được `null` | 🟠 Vừa | Tập trung phân giải quyền lợi vào **1 service** (`SubscriptionAccessService`); cấm đọc `user.getCurrentSubscription()` rải rác |
| **R5** | `ddl-auto=update` KHÔNG ALTER/DROP/rename được | `CLAUDE.md §5`. Đổi kiểu/đổi tên `resetAt`, hay thêm cột `NOT NULL` lên bảng đã có dữ liệu → fail hoặc bị bỏ qua âm thầm | 🟠 Vừa | **Chỉ thêm cột `nullable`/có default.** Không rename tại chỗ. Dữ liệu cũ vá bằng backfill trong `DataInitializer` |
| **R6** | `DataInitializer` chạy lúc startup (`@Order(2)`) | `initializer/DataInitializer.java`. 1 backfill ném exception → **app không khởi động được** | 🟠 Vừa | Mỗi backfill phải **idempotent + null-guard + try/catch log**; test trên bản copy DB trước |
| **R7** | `PlanFeature` lazy trong `aiLimitOf()` | `QuotaServiceImpl.aiLimitOf()` gọi `plan.getPlanFeatures().stream()`. An toàn hiện tại vì method `@Transactional`; nhưng nếu tái dùng ở chỗ non-tx → nổ như R1 | 🟡 Thấp | Giữ mọi read quota trong `@Transactional`; snapshot `nominalAiLimit` vào `UserQuota` để runtime check khỏi chạm `PlanFeature` |
| **R8** | `RateLimiterService` / `PlanLimitService` phân giải plan hiện tại | Nếu base user không có sub → resolve plan = `null` → NPE / giới hạn sai | 🟠 Vừa | Fallback base plan tập trung, gate bằng feature flag; test case "user không sub" |
| **R9** | Code path mới không gate bằng feature flag | Deploy code mới mà không cờ → đổi hành vi baseline ngay lập tức | 🔴 Cao | **Mọi task đổi hành vi phải đọc flag, default OFF.** Flag OFF = chạy y hệt baseline |

---

## 7. Các bước làm dần để HẠN CHẾ NỔ (có hướng dẫn cách làm)

> Nguyên tắc thứ tự: **rủi ro tăng dần**. Làm hết nhóm an toàn trước, mỗi bước tự compile/test được,
> rollback độc lập. Không nhảy cóc sang cutover khi nền chưa chắc.

### P0 — Ổn định ngay (không đổi schema, không đổi hành vi công khai)

| Bước | Việc | File | Cách làm | Kiểm chứng | Rollback |
|---|---|---|---|---|---|
| P0.1 | Commit fix quota + backfill đang treo | `QuotaServiceImpl.java`, `DataInitializer.java`, `QuotaServiceImplTest.java` | `git add -A` → commit trên nhánh `feat/admin-charts` → push. (Fix đã xong, chỉ chưa lưu) | `mvnw test` xanh; team pull về hết `9999` | `git revert` |
| P0.2 | Fix LazyInit metric job (R1/F4) | `service/scheduleJobs/Impl/SaasMetricSyncServiceImpl.java` | Thêm `@Transactional(readOnly = true)` lên `syncMetrics()` **hoặc** đổi `findByStatus(ACTIVE)` sang query `JOIN FETCH s.plan`. Ưu tiên JOIN FETCH để không giữ tx dài | Chạy job (hoặc gọi tay) không còn `LazyInitializationException` | Revert 1 file |
| P0.3 | Gộp cấp-subscription idempotent (R3/F5) | `service/Impl/PaymentServiceImpl.java` | Tách logic `processWebhook`+`getPaymentStatus` thành `private void activate(PaymentTransaction t)`. Trước khi cấp: `UPDATE payment_transactions SET status=PAID WHERE order_code=? AND status=PENDING` → chỉ cấp khi **1 row** đổi. Cả 2 caller gọi `activate()` | Test: gọi webhook + polling cùng orderCode → **chỉ 1** Subscription được tạo | Revert 1 file (2 caller giữ nguyên nếu cần) |

### P1 — Nền additive (thêm field/enum/method, KHÔNG code nào gọi tới → 0 đổi hành vi)

| Bước | Việc | File | Cách làm | Kiểm chứng | Rollback |
|---|---|---|---|---|---|
| P1.1 | Thêm `Plan.tierOrder` (chốt field chu kỳ trước — R2/F3) | `entity/Plan.java` | Thêm `@Column(name="tier_order") Integer tierOrder;` (**nullable**). **Chưa** thêm `billingCycle`/`quotaPeriodDays` cho tới khi chốt F3 để tránh 4 field trùng | `mvnw compile`; cột tự tạo qua ddl-auto | `git revert` |
| P1.2 | Backfill `tier_order` cho seed plan | `initializer/DataInitializer.java` | Thêm `backfillTierOrder()` idempotent: `UPDATE plans SET tier_order=? WHERE id=? AND tier_order IS NULL` cho Free=0…Pro=3 (UUID `1111…`→`4444…`) | Khởi động 1 lần → 4 plan có tier; chạy lại update 0 row | Revert method |
| P1.3 | Thêm enum còn thiếu | `enums/SubscriptionStatus.java`, `enums/PaymentStatus.java`, NEW `enums/PaymentTransactionType.java` | Thêm `REPLACED`, `REQUIRES_REVIEW`, và enum `{NEW_SUBSCRIPTION, UPGRADE}`. Không xóa giá trị cũ | `mvnw compile` | `git revert` |
| P1.4 | Thêm snapshot field (nullable) | `entity/Subscription.java`, `entity/UserQuota.java`, `entity/PaymentTransaction.java` | Chỉ `@Column(nullable=true)`; **không** rename `resetAt` (R5). `UserQuota` thêm `nominalAiLimit/effectiveAiLimit/planId` nhưng **tạm chưa dùng** | `mvnw compile`; cột mới toàn null | `git revert` |
| P1.5 | Thêm repository method (chưa ai gọi) | `SubscriptionRepository.java`, `UserQuotaRepository.java` | Thêm query `findActive...`, `findByUserIdAnd...`. Method cũ giữ nguyên | `mvnw compile` | `git revert` |

### P2 — Logic thuần + read-only sau feature flag (đổi rất ít, dễ tắt)

| Bước | Việc | File | Cách làm | Kiểm chứng | Rollback |
|---|---|---|---|---|---|
| P2.1 | `UpgradeCalculator` thuần | NEW `service/UpgradeCalculator.java` + test | Hàm tĩnh, **0 DB, 0 dependency**: `calculate(subSnapshot, targetPlan, quota, now)`. Money `BigDecimal` HALF_UP scale 0; quota delta FLOOR; clamp [0,1] | Unit test 10 case (còn/hết quota, 1/30, 100%, sequential…) | Xóa file |
| P2.2 | Endpoint quote (flag OFF) | NEW `controller/QuoteController.java`, `service/UpgradeQuoteService.java` | `POST /subscriptions/upgrade/quote`. Đọc `feature.quote-v2-enabled` (default `false`) → OFF trả 404. **Read-only**, không tạo payment | Flag OFF: 404. Bật local: trả quote đúng số Calculator | Tắt flag / revert |

### P3 — Cutover (rủi ro cao — chỉ làm khi P0–P2 đã chắc)

| Bước | Việc | Điều kiện tiên quyết | Cách làm an toàn |
|---|---|---|---|
| P3.1 | Chặn same-plan/downgrade + eligibility mua | P0.3 xong (idempotent) | Thêm sau flag `entitlement-v2-enabled=false`; dùng `tierOrder` (P1.1) so sánh; ErrorCode mới |
| P3.2 | Base user không tạo Subscription (R4) | `SubscriptionAccessService` tập trung xong | Gate flag; test kỹ path `currentSubscription == null` ở Quota/RateLimiter/PlanLimit |
| P3.3 | Quota chuyển nominal/effective (R2) | Đã chốt cơ chế period | Nếu flag OFF → giữ `resetAt` cũ; ON → dùng field mới. **Không** chạy 2 cơ chế cùng lúc |
| P3.4 | Webhook dùng `SubscriptionActivationService` | P0.3 là bản rút gọn của bước này | Cùng release FE; canary 5% → monitor → 100% |

> **Chốt trước khi vào P1:** (a) cơ chế quota giữ `resetAt` hay chuyển period (R2); (b) field chu kỳ chuẩn là gì (F3). Hai quyết định này chặn P1.1/P1.4 — làm sai phải sửa schema (R5 rất đau).
