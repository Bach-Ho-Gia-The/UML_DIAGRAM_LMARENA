# IMPLEMENTATION PLAN — Subscription / Upgrade / Proration Phase 1

> **Baseline commit:** `6a30e890a1900d5de36162f57c5534500131b49b`  
> **Ngôn ngữ:** Tiếng Việt  
> **Nguyên tắc:** Additive schema, feature flag, rollback-safe, mỗi task compile/test được độc lập

---

## Mục lục

1. [Dependency Graph](#1-dependency-graph)
2. [Feature Flag Map](#2-feature-flag-map)
3. [Task Plan](#3-task-plan)
   - [Chặng 0: Baseline stabilization](#ch%E1%BA%B7ng-0-baseline-stabilization-1-task)
   - [Chặng 1A: Entity additive fields](#ch%E1%BA%B7ng-1a-entity-additive-fields-7-tasks)
   - [Chặng 1B: Repository additive methods](#ch%E1%BA%B7ng-1b-repository-additive-methods-4-tasks)
   - [Chặng 1C: Pure Calculator + Quote](#ch%E1%BA%B7ng-1c-pure-calculator--quote-6-tasks)
   - [Chặng 2A: Payment/Subscription preparation](#ch%E1%BA%B7ng-2a-paymentsubscription-preparation-7-tasks)
   - [Chặng 2B: Entitlement cutover](#ch%E1%BA%B7ng-2b-entitlement-cutover-6-tasks)
   - [Chặng 3: Expiry/Fallback + Metrics](#ch%E1%BA%B7ng-3-expiryfallback--metrics-3-tasks)
   - [Chặng 4A: Compliance Guard](#ch%E1%BA%B7ng-4a-compliance-guard-9-tasks)
   - [Chặng 4B: Archive + Selection](#ch%E1%BA%B7ng-4b-archive--selection-7-tasks)
   - [Chặng 5: Purge enablement](#ch%E1%BA%B7ng-5-purge-enablement-4-tasks)
4. [Open Decisions & Defaults](#4-open-decisions--defaults)
5. [Rollback Matrix](#5-rollback-matrix)
6. [File Manifest](#6-file-manifest)

---

## 1. Dependency Graph

```
                    ┌──────────────────────────────────┐
                    │          Entity Layer             │
                    │  (Plan, Subscription, UserQuota,  │
                    │   User, Project, Sheet,           │
                    │   PaymentTransaction, NEW:        │
                    │   EntitlementGracePeriod)         │
                    └──────────┬───────────────────────┘
                               │
                    ┌──────────▼───────────────────────┐
                    │        Repository Layer           │
                    │  (JPA interfaces + @Query)       │
                    └──────────┬───────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
┌─────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  PlanLimitService │  │  QuotaService    │  │  PaymentService  │
│  (capacity check) │  │  (AI quota)      │  │  (payment flow)  │
└────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
         │                     │                      │
         ▼                     ▼                      ▼
┌─────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  NEW:            │  │  NEW:             │  │  NEW:             │
│ EntitlementService│  │ QuotaPeriodService│  │ SubscriptionActi- │
│ (coordinator)    │  │ (period mgmt)    │  │ vationService     │
└────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
         │                     │                      │
         ▼                     ▼                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    Service Consumers                          │
│  (ProjectService, SheetService, WorkspaceItemService,        │
│   DiagramVersionService, DiagramChatService,                 │
│   SocketService, RateLimiterService)                         │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    Controllers (API layer)                   │
│  (ProjectController, SheetController, etc.)                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Feature Flag Map

```yaml
# application.yaml — mặc định OFF, bật canary từng cái
feature:
  quote-v2-enabled: false          # Chặng 1C — toggle quote endpoint
  entitlement-v2-enabled: false    # Chặng 2  — toggle subscription/entitlement logic
  compliance-guard-enabled: false  # Chặng 4A — toggle ComplianceGuard
  archive-enabled: false           # Chặng 4B — toggle archive/restore APIs
  purge-enabled: false             # Chặng 5  — toggle purge job (mặc định OFF vĩnh viễn!)
```

**Rollback:** tắt flag = code cũ chạy y hệt baseline.

---

## 3. Task Plan

### CHẶNG 0: Baseline stabilization (1 task)

| # | Task | Files | Thay đổi | Compile | Rollback |
|---|------|-------|----------|---------|----------|
| 0.1 | Fix `DiagramChatServiceImpl` interface mismatch | `DiagramChatServiceImpl.java` | Implement method signature khớp với interface pagination | ✅ | ✅ `git revert` |

---

### CHẶNG 1A: Entity additive fields (7 tasks)

> Chỉ thêm field `nullable`, KHÔNG sửa field cũ, KHÔNG sửa constructor/builder pattern cũ.

| # | Entity | Thêm field | Default | Ghi chú |
|---|--------|-----------|---------|---------|
| 1.1 | `Plan.java` | `tierOrder INTEGER`, `billingCycle VARCHAR(20)`, `quotaPeriodDays INTEGER` | null | `@Column(nullable = true)` |
| 1.2 | `Subscription.java` | `billingPriceSnapshot NUMERIC`, `currencySnapshot VARCHAR(10)`, `billingCycleSnapshot VARCHAR(20)`, `nominalAiLimitSnapshot INTEGER`; status `REPLACED` | null | Thêm `REPLACED` vào enum |
| 1.3 | `UserQuota.java` | `nominalAiLimit INTEGER`, `effectiveAiLimit INTEGER`, `quotaPeriodStart TIMESTAMP`, `quotaPeriodEnd TIMESTAMP`, `planId UUID` | null | Field cũ `aiLimit`/`resetAt` giữ nguyên |
| 1.4 | `PaymentTransaction.java` | `type VARCHAR(20)`, `sourceSubscriptionId UUID`, `sourcePlanId UUID`, `targetPlanId UUID`, `oldPriceSnapshot NUMERIC`, `newPriceSnapshot NUMERIC`, `priceDifference NUMERIC`, `billingRemainingRatio NUMERIC`, `quotaRemainingRatio NUMERIC`, `oldNominalQuota INTEGER`, `newNominalQuota INTEGER`, `quotaDelta INTEGER`, `newEffectiveLimit INTEGER`, `quoteCreatedAt TIMESTAMP`, `quoteExpiresAt TIMESTAMP`; status `REQUIRES_REVIEW` | null | Thêm `REQUIRES_REVIEW` vào enum |
| 1.5 | `Project.java` | `lifecycleStatus VARCHAR(30)`, `archivedReason VARCHAR(255)`, `archivedAt TIMESTAMP`, `purgeAt TIMESTAMP` | `'ACTIVE'` | `@Column(columnDefinition = "VARCHAR(30) DEFAULT 'ACTIVE'")` |
| 1.6 | `Sheet.java` | `lifecycleStatus VARCHAR(30)`, `archivedAt TIMESTAMP`, `purgeAt TIMESTAMP` | `'ACTIVE'` | Đồng bộ với Project |
| 1.7 | `EntitlementGracePeriod.java` (NEW) | Entity mới: `id UUID`, `userId UUID`, `sourcePlanId UUID`, `targetPlanId UUID`, `status VARCHAR(20)`, `startedAt TIMESTAMP`, `endsAt TIMESTAMP`, `resolvedAt TIMESTAMP`, `archivedAt TIMESTAMP`, `warningDay0SentAt TIMESTAMP`, `warningDay3SentAt TIMESTAMP`, `warningDay6SentAt TIMESTAMP`, `warningDay7SentAt TIMESTAMP`, `projectCountSnapshot INTEGER`, `diagramCountSnapshot INTEGER`, `projectLimitSnapshot INTEGER`, `diagramLimitSnapshot INTEGER`, `selectionConfirmedAt TIMESTAMP` | — | Entity hoàn toàn mới |

---

### CHẶNG 1B: Repository additive methods (4 tasks)

> Method cũ giữ nguyên. Chỉ thêm method mới.

| # | Repository | Method mới | Notes |
|---|-----------|-----------|-------|
| 1.8 | `SubscriptionRepository.java` | `Optional<Subscription> findByUserAndStatusInAndStartDateBeforeAndEndDateAfter(UUID userId, List<String> statuses, LocalDateTime now, LocalDateTime now2)` | Tìm ACTIVE subscription thực sự hiệu lực |
| 1.9 | `UserQuotaRepository.java` | `Optional<UserQuota> findByUserIdAndQuotaPeriodEndAfter(UUID userId, LocalDateTime now)` | Tìm quota period hiện tại |
| 1.10 | `ProjectRepository.java` | `Page<Project> findAllByUserAndIsDeletedFalseAndLifecycleStatus(UUID userId, Pageable pageable, String lifecycleStatus)`, `List<Project> findByUserIdAndLifecycleStatus(UUID userId, String status)`, `long countByUserIdAndLifecycleStatus(UUID userId, String status)` | Lifecycle-aware queries |
| 1.11 | `EntitlementGracePeriodRepository.java` (NEW) | `Optional<EntitlementGracePeriod> findByUserIdAndStatus(UUID userId, String status)`, `List<EntitlementGracePeriod> findByStatusAndEndsAtBefore(String status, LocalDateTime now)` | Repository mới |

---

### CHẶNG 1C: Pure Calculator + Quote (6 tasks)

> **Feature flag:** `feature.quote-v2-enabled = false` → endpoint trả 404  
> Tất cả code mới, không sửa code cũ.

| # | Task | Files | Mô tả |
|---|------|-------|-------|
| 1.12 | NEW: UpgradeCalculator (pure) | `service/UpgradeCalculator.java` | Hàm thuần: `calculate(currentSubSnapshot, targetPlan, quota, now) → UpgradeQuote`. 0 dependency, 0 DB. |
| 1.13 | Unit test: UpgradeCalculator | `src/test/.../UpgradeCalculatorTest.java` | 10 cases: còn quota, hết quota, 1/30, 100%, sequential, FLOOR/HALF_UP, clamp, cross-cycle reject |
| 1.14 | NEW: DTOs cho quote | `dto/UpgradeQuoteRequest.java`, `dto/UpgradeQuoteResponse.java`, `dto/SubscriptionSnapshot.java`, `dto/PlanSnapshot.java`, `dto/QuotaSnapshot.java` | Immutable snapshots, `@Data @Builder ...` |
| 1.15 | NEW: UpgradeQuoteService | `service/UpgradeQuoteService.java` | Gọi `SubscriptionAccessService.getActiveSubscription()`, `QuotaPeriodService.ensurePeriod()`, `UpgradeCalculator`. Read-only. |
| 1.16 | NEW: SubscriptionAccessService | `service/SubscriptionAccessService.java` | `getActiveSubscription(userId, now)` — query repository mới |
| 1.17 | NEW: QuotaPeriodService | `service/QuotaPeriodService.java` | `ensurePeriod(userId)`, `advanceIfExpired(userId)` — lazy reset, xử lý skip nhiều kỳ |
| 1.18 | NEW: QuoteController | `controller/QuoteController.java` | `POST /subscriptions/upgrade/quote` — feature flag OFF → 404 |

---

### CHẶNG 2A: Payment/Subscription preparation (7 tasks)

> Feature flag: `feature.entitlement-v2-enabled = false`.  
> Code mới additive, không sửa flow cũ.

| # | Task | Files | Mô tả |
|---|------|-------|-------|
| 2.1 | NEW: PaymentTransactionType enum | `enums/PaymentTransactionType.java` | `NEW_SUBSCRIPTION`, `UPGRADE` |
| 2.2 | NEW: UpgradePaymentService | `service/UpgradePaymentService.java` | Phân loại intent (NEW/UPGRADE/reject), snapshot quote, chặn duplicate PENDING, gọi PayOS |
| 2.3 | NEW: SubscriptionActivationService | `service/SubscriptionActivationService.java` | `activateNewSubscription(user, payment)`, `activateUpgrade(user, payment, oldSub)` — pessimistic lock, idempotent |
| 2.4 | PaymentServiceImpl: thêm nhánh UPGRADE | `service/Impl/PaymentServiceImpl.java` | Webhook handler: verify → phân loại → gọi `SubscriptionActivationService` thay vì tự tạo Subscription |
| 2.5 | PaymentController: thêm endpoint | `controller/PaymentController.java` | `POST /payments/create`: auto-phân loại intent |
| 2.6 | PlanLimitService: thêm overload | `service/Impl/PlanLimitServiceImpl.java` | `assertCanCreate(userId, key, count, entitlementService)` — nếu flag OFF dùng logic cũ |
| 2.7 | RateLimiterService: thêm fallback | `service/Impl/RateLimiterServiceImpl.java` | Fallback base plan nếu không có subscription |

---

### CHẶNG 2B: Entitlement cutover (6 tasks)

> **Cùng release, feature flag `entitlement-v2-enabled`.**
> Bật canary 5% → monitor → 100%.

| # | Task | Files | Rủi ro | Mô tả |
|---|------|-------|--------|-------|
| 2.8 | UserServiceImpl: không tạo Subscription cho base user | `service/Impl/UserServiceImpl.java` | **CRITICAL** | `registerUser()`: nếu plan = base → không tạo Subscription, chỉ tạo UserQuota với `subscriptionId=null` |
| 2.9 | CustomOAuth2UserServiceImpl: tương tự | `service/Impl/CustomOAuth2UserServiceImpl.java` | **CRITICAL** | Google OAuth2 user: không tạo Subscription cho base plan |
| 2.10 | QuotaServiceImpl: refactor read quota | `service/Impl/QuotaServiceImpl.java` | **HIGH** | Nếu flag OFF → giữ code cũ. Nếu ON → `SubscriptionAccessService` → null → base Plan features. Field mới `effectiveAiLimit`/`nominalAiLimit` trong response |
| 2.11 | QuotaController: thêm field mới | `controller/QuotaController.java`, `dto/response/QuotaResponse.java` | Thấp | Backward-compatible: field mới optional, FE cũ bỏ qua |
| 2.12 | PaymentServiceImpl: old purchase flow removed | `service/Impl/PaymentServiceImpl.java` | **CRITICAL** | Giữ cả 2 code path, flag quyết định. Khi flag ON → webhook dùng activation service mới |
| 2.13 | AdminController: guard base Plan | `controller/AdminController.java` | Trung | Không cho archive/delete base Plan ACTIVE |

---

### CHẶNG 3: Expiry/Fallback + Metrics (3 tasks)

| # | Task | Files | Mô tả |
|---|------|-------|-------|
| 3.1 | NEW: SubscriptionExpiryJob | `service/scheduleJobs/SubscriptionExpiryJob.java` | `@Scheduled` + ShedLock: tìm subscription `ACTIVE` hết hạn → `EXPIRED`, fallback base, gọi `QuotaPeriodService` |
| 3.2 | DashboardServiceImpl: MRR metrics | `service/Impl/DashboardServiceImpl.java` | Cập nhật MRR/churn query dùng `SubscriptionAccessService` |
| 3.3 | DashboardController: backward-compatible | `controller/DashboardController.java` | Response giữ field cũ |

---

### CHẶNG 4A: Compliance Guard (9 tasks)

> **Feature flag:** `feature.compliance-guard-enabled = false` → guard bỏ qua.

| # | Task | Files | Mô tả |
|---|------|-------|-------|
| 4.1 | NEW: WorkspaceOperation enum | `enums/WorkspaceOperation.java` | `READ, EXPORT, DELETE, SELECT_ACTIVE_DATA, PAYMENT, PROFILE, MUTATE_PROJECT, MUTATE_DIAGRAM, MUTATE_WORKSPACE_TREE, AI_MUTATION, COLLABORATION_MUTATION, RESTORE_ARCHIVE` |
| 4.2 | NEW: ComplianceGuard | `service/ComplianceGuard.java` | `check(userId, operation, context)` — throw `PLAN_RECONCILIATION_REQUIRED` nếu đang reconciliation và operation bị block |
| 4.3 | NEW: GracePeriodService | `service/GracePeriodService.java` | Tạo/update/resolve reconciliation, nhận selection, archive unselected |
| 4.4 | ProjectServiceImpl: guard | `service/Impl/ProjectServiceImpl.java` | Gọi `ComplianceGuard.check(ownerId, MUTATE_PROJECT)` trong mutation methods |
| 4.5 | SheetServiceImpl: guard | `service/Impl/SheetServiceImpl.java` | Gọi `ComplianceGuard.check(ownerId, MUTATE_DIAGRAM)` |
| 4.6 | WorkspaceItemServiceImpl: guard | `service/Impl/WorkspaceItemServiceImpl.java` | Gọi `ComplianceGuard.check(ownerId, MUTATE_WORKSPACE_TREE)` |
| 4.7 | DiagramVersionServiceImpl: guard | `service/Impl/DiagramVersionServiceImpl.java` | Gọi `ComplianceGuard.check(ownerId, ...)` |
| 4.8 | DiagramChatServiceImpl: guard | `service/Impl/DiagramChatServiceImpl.java` | Gọi `ComplianceGuard.check(ownerId, AI_MUTATION)` |
| 4.9 | SocketServiceImpl: guard | `service/Impl/SocketServiceImpl.java` | Gọi `ComplianceGuard.check(ownerId, COLLABORATION_MUTATION)` |

**⚠️ Critical path:** `ComplianceGuard` phải lấy đúng `ownerId` (project owner, không phải actor), nếu sai → block nhầm hoặc không block. Test matrix bắt buộc: owner, collaborator, admin, anonymous.

---

### CHẶNG 4B: Archive + Selection (7 tasks)

> **Feature flag:** `feature.archive-enabled = false` → API trả 404.

| # | Task | Files | Mô tả |
|---|------|-------|-------|
| 4.10 | NEW: ArchiveService | `service/ArchiveService.java` | `archiveProject(project, reason)`, `archiveSheet(sheet, reason)`, `restore(userId, itemId, type)` |
| 4.11 | NEW: ComplianceController (QuoteController mở rộng) | `controller/ComplianceController.java` | `GET /me/subscription/compliance`, `POST /me/subscription/compliance/selection` |
| 4.12 | NEW: ArchiveController | `controller/ArchiveController.java` | `GET /me/archive`, `POST /me/archive/{id}/restore`, `GET /me/archive/{id}/export` |
| 4.13 | NEW: GracePeriodWarningJob | `service/scheduleJobs/GracePeriodWarningJob.java` | Email + in-app warning ngày 0, 3, 6, 7 |
| 4.14 | NEW: GracePeriodDeadlineJob | `service/scheduleJobs/GracePeriodDeadlineJob.java` | Hết 7 ngày → archive toàn bộ nếu chưa selection |
| 4.15 | ProjectController: filter archive | `controller/ProjectController.java` | Dùng method mới, loại `ARCHIVED_OVER_LIMIT` khỏi normal list |
| 4.16 | QuotaController: thêm capacity info | `controller/QuotaController.java` | Thêm `projectCount`, `diagramCount` trong response |

---

### CHẶNG 5: Purge enablement (4 tasks)

> **Feature flag:** `feature.purge-enabled = false` (mặc định OFF).

| # | Task | Files | Mô tả |
|---|------|-------|-------|
| 5.1 | NEW: ArchiveWarningJob | `service/scheduleJobs/ArchiveWarningJob.java` | Warning 7 ngày + 24h trước purge |
| 5.2 | NEW: ArchivePurgeJob | `service/scheduleJobs/ArchivePurgeJob.java` | Re-check state → domain cleanup graph → audit → email |
| 5.3 | Integration test: delete graph | `src/test/.../ArchivePurgeJobTest.java` | Verify domain cleanup đúng thứ tự: Sheet → DiagramVersion → WorkspaceItem → Project |
| 5.4 | Bật feature flag canary | `application.yaml` | Canary 5% → 100% sau monitor |

---

## 4. Open Decisions & Defaults

Các decisions chưa chốt — tôi đặt default hợp lý để plan có thể chạy. Review và override nếu cần.

| ID | Question | Default | Lý do |
|----|----------|---------|-------|
| **MIGRATION-01** | Flyway hay V002 manual? | V002 manual SQL (giữ nguyên format cũ) | Không thêm dependency; dev DB disposable |
| **QUOTE-01** | Auto-gen hay user-initiated? | User-initiated (call `POST /quote`) | Clean separation; không side-effect |
| **PAYMENT-01** | Snapshot lưu trực tiếp hay tính lại? | Snapshot vào `PaymentTransaction` tại thời điểm tạo payment | Audit trail; price change không ảnh hưởng transaction cũ |
| **PAYMENT-02** | PayOS webhook event type mới? | New type `UPGRADE` handler | Không break existing payment flow |
| **PLAN-01** | `tierOrder` set manual hay auto? | Manual trong DB seed | Explicit control, không phụ thuộc price |
| **ARCHIVE-01** | Cùng bảng hay tách bảng? | Cùng bảng + `lifecycleStatus` | Additive, rollback-safe, không migration phức tạp |
| **FILE-01** | Cold storage cho archive? | Cùng storage, chỉ đánh dấu `lifecycleStatus` | Đơn giản, rollback-safe |
| **CHAT-01** | Chat messages giữ hay xóa? | Giữ, đánh dấu `lifecycleStatus` (thêm field `archivedAt` trên ChatSession) | Data preservation; không mất context |
| **EXPORT-01** | Export định dạng gì? | Cả JSON + `.drawio` (user chọn) | FE flexibility |
| **PURGE-01** | Grace archive bao nhiêu ngày? | Configurable: default 30 days | Flexible, có thể điều chỉnh không cần code change |

---

## 5. Rollback Matrix

| Tình huống | Hành động | Hậu quả |
|-----------|-----------|---------|
| Task 1.1–1.7 lỗi (entity) | `git revert` task đó | Mất field mới, code cũ vẫn chạy |
| Task 1.8–1.11 lỗi (repository) | `git revert` | Query mới biến mất, code cũ không gọi đến |
| Task 2.8 lỗi (register không tạo sub) | **Tắt flag `entitlement-v2-enabled`** → code cũ tạo sub như cũ | User mới trong khoảng lỗi không có sub → manual backfill |
| Task 2.12 lỗi (payment flow) | **Tắt flag `entitlement-v2-enabled`** → dùng payment flow cũ | Payment trong khoảng lỗi phải manual review |
| Task 4.4–4.9 lỗi (ComplianceGuard) | **Tắt flag `compliance-guard-enabled`** → mutation không bị guard | User trong reconciliation có thể edit — tạm chấp nhận được đến khi fix |
| Task 5.2 lỗi (Purge) | **Không bật flag** → purge không chạy | Archive tồn đọng, không bị mất |

**Nguyên tắc:** Feature flag OFF = code path cũ chạy y hệt baseline. Deploy code mới với flag OFF không gây khác biệt behavior.

---

## 6. File Manifest

### Files mới tạo (27 files)

| File | Chặng |
|------|-------|
| `service/UpgradeCalculator.java` | 1C |
| `service/UpgradeQuoteService.java` | 1C |
| `service/SubscriptionAccessService.java` | 1C |
| `service/QuotaPeriodService.java` | 1C |
| `service/UpgradePaymentService.java` | 2A |
| `service/SubscriptionActivationService.java` | 2A |
| `service/ComplianceGuard.java` | 4A |
| `service/GracePeriodService.java` | 4A |
| `service/ArchiveService.java` | 4B |
| `service/Impl/*.java` (impls) | (tương ứng) |
| `controller/QuoteController.java` | 1C |
| `controller/ComplianceController.java` | 4B |
| `controller/ArchiveController.java` | 4B |
| `entity/EntitlementGracePeriod.java` | 1A |
| `enums/PaymentTransactionType.java` | 2A |
| `enums/WorkspaceOperation.java` | 4A |
| `dto/UpgradeQuoteRequest.java` | 1C |
| `dto/UpgradeQuoteResponse.java` | 1C |
| `dto/SubscriptionSnapshot.java` | 1C |
| `dto/PlanSnapshot.java` | 1C |
| `dto/QuotaSnapshot.java` | 1C |
| `repository/EntitlementGracePeriodRepository.java` | 1B |
| `service/scheduleJobs/SubscriptionExpiryJob.java` | 3 |
| `service/scheduleJobs/GracePeriodWarningJob.java` | 4B |
| `service/scheduleJobs/GracePeriodDeadlineJob.java` | 4B |
| `service/scheduleJobs/ArchiveWarningJob.java` | 5 |
| `service/scheduleJobs/ArchivePurgeJob.java` | 5 |

### Files sửa đổi (30+ files)

| File | Chặng | Thay đổi |
|------|-------|----------|
| `entity/Plan.java` | 1A | + tierOrder, billingCycle, quotaPeriodDays |
| `entity/Subscription.java` | 1A | + snapshot fields, REPLACED status |
| `entity/UserQuota.java` | 1A | + nominal/effective limit, period, planId |
| `entity/PaymentTransaction.java` | 1A | + upgrade fields, REQUIRES_REVIEW |
| `entity/Project.java` | 1A | + lifecycleStatus, archivedAt, purgeAt |
| `entity/Sheet.java` | 1A | + lifecycleStatus, archivedAt, purgeAt |
| `repository/SubscriptionRepository.java` | 1B | + findActive query |
| `repository/UserQuotaRepository.java` | 1B | + period query |
| `repository/ProjectRepository.java` | 1B | + lifecycle queries |
| `service/Impl/QuotaServiceImpl.java` | 2B | + read quota (base/paid) |
| `service/Impl/PlanLimitServiceImpl.java` | 2A | + overload method |
| `service/Impl/PaymentServiceImpl.java` | 2A/2B | + upgrade payment path, flag cutover |
| `service/Impl/RateLimiterServiceImpl.java` | 2A | + fallback base plan |
| `service/Impl/ProjectServiceImpl.java` | 4A | + ComplianceGuard |
| `service/Impl/SheetServiceImpl.java` | 4A | + ComplianceGuard |
| `service/Impl/WorkspaceItemServiceImpl.java` | 4A | + ComplianceGuard |
| `service/Impl/DiagramVersionServiceImpl.java` | 4A | + ComplianceGuard |
| `service/Impl/DiagramChatServiceImpl.java` | 0+4A | Fix interface + ComplianceGuard |
| `service/Impl/SocketServiceImpl.java` | 4A | + ComplianceGuard |
| `service/Impl/DashboardServiceImpl.java` | 3 | + MRR metrics |
| `service/Impl/UserServiceImpl.java` | 2B | + không tạo sub cho base user |
| `service/Impl/CustomOAuth2UserServiceImpl.java` | 2B | + không tạo sub cho base user |
| `controller/ProjectController.java` | 4B | + Archive filter query |
| `controller/AdminController.java` | 2B | + guard base Plan |
| `controller/QuotaController.java` | 2B+4B | + new fields, capacity info |
| `controller/DashboardController.java` | 3 | backward-compatible |
| `controller/PaymentController.java` | 2A | + upgrade endpoint |
| `dto/response/QuotaResponse.java` | 2B | + new optional fields |
| `exception/ErrorCode.java` | multiple | + 20+ error codes |
| `docs/migrations/V002__subscription_upgrade_phase1.sql` | 1A | SQL migration script |
