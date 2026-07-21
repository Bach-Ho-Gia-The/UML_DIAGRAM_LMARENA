package su26.uml.be.dto.subscription;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * Snapshot quota kỳ hiện tại — input cho {@link su26.uml.be.service.UpgradeCalculator}.
 * Chặng 1C lấy từ cơ chế {@code resetAt} hiện có (xem planing.md R2 — 1 cơ chế period duy nhất).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuotaSnapshot {
    int used;
    int effectiveLimit;
    LocalDateTime periodStart;
    LocalDateTime periodEnd;
}
