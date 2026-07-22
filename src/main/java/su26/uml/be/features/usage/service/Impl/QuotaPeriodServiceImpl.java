package su26.uml.be.features.usage.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import su26.uml.be.features.usage.dto.QuotaResponse;
import su26.uml.be.features.subscription.dto.QuotaSnapshot;
import su26.uml.be.features.usage.service.QuotaPeriodService;
import su26.uml.be.features.usage.service.QuotaService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuotaPeriodServiceImpl implements QuotaPeriodService {

    QuotaService quotaService;

    @Value("${quota.period-days:30}")
    @NonFinal
    int periodDays;

    @Override
    public QuotaSnapshot getCurrentSnapshot(UUID userId) {
        // Đọc quota hiện tại (đã lazy-reset theo resetAt) — 1 cơ chế period duy nhất (R2).
        QuotaResponse q = quotaService.getQuota(userId);
        return QuotaSnapshot.builder()
                .used(q.getUsed())
                .effectiveLimit(q.getLimit())
                .periodEnd(q.getResetAt())
                // Phase 1 kỳ cố định periodDays → suy periodStart từ resetAt (đủ cho tính tỉ lệ).
                .periodStart(q.getResetAt() == null ? null : q.getResetAt().minusDays(periodDays))
                .build();
    }
}