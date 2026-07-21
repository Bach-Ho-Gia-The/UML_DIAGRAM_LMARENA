package su26.uml.be.service;

import su26.uml.be.dto.subscription.QuotaSnapshot;

import java.util.UUID;

/**
 * Cung cấp snapshot quota kỳ hiện tại cho luồng quote (Chặng 1C).
 *
 * <p><b>R2 (planing.md):</b> Chặng 1C KHÔNG tạo cơ chế period song song — impl đọc lại quota qua
 * {@link QuotaService} (đã áp lazy reset theo {@code resetAt}) và suy ra period. Việc quản lý
 * {@code quotaPeriodStart/End} mới (ensure/advance) để dành Chặng 2B sau khi chốt cơ chế period.
 */
public interface QuotaPeriodService {

    /** Snapshot quota kỳ hiện tại của user (used / effectiveLimit / periodStart / periodEnd). */
    QuotaSnapshot getCurrentSnapshot(UUID userId);
}
