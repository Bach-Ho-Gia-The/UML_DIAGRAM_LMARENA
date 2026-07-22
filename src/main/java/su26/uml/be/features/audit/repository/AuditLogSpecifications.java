package su26.uml.be.features.audit.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import su26.uml.be.features.audit.entity.AuditLog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Specification để lọc động {@code audit_log} theo action / actor / khoảng thời gian.
 * Sắp xếp (createdAt DESC) do {@code Pageable} đảm nhiệm ở tầng service.
 */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    /**
     * @param action   lọc đúng action (bỏ qua nếu null/blank)
     * @param actorId  lọc theo UUID admin (bỏ qua nếu null)
     * @param fromInclusive mốc bắt đầu (>=), đã quy đổi sang Instant (bỏ qua nếu null)
     * @param toExclusive   mốc kết thúc (<), đã quy đổi sang Instant (bỏ qua nếu null)
     */
    public static Specification<AuditLog> withFilters(
            String action, UUID actorId, Instant fromInclusive, Instant toExclusive) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (actorId != null) {
                predicates.add(cb.equal(root.get("actorId"), actorId));
            }
            if (fromInclusive != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromInclusive));
            }
            if (toExclusive != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), toExclusive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}