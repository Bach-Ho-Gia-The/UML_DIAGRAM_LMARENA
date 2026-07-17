package su26.uml.be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import su26.uml.be.enums.PlanStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "plans")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Plan extends BaseEntity {

    @Column(nullable = false)
    String name;

    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal price;

    @Column(nullable = false, length = 10, columnDefinition = "varchar(10) default 'VND'")
    @Builder.Default
    String currency = "VND";

    @Column(columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'DRAFT'")
    @Builder.Default
    PlanStatus status = PlanStatus.DRAFT;

    @Column(length = 20)
    String color;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    boolean popular = false;

    @Column(name = "yearly_billing", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    boolean yearlyBilling = false;

    @Column(name = "yearly_discount")
    @Builder.Default
    Integer yearlyDiscount = 0;

    /** When true, the pricing card shows "Liên hệ báo giá" instead of a price (Enterprise). Distinct from price = 0 (Free). */
    @Column(name = "contact_sales", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    boolean contactSales = false;

    @Column(name = "max_diagrams")
    Integer maxDiagrams; // -1 for unlimited (legacy; superseded by plan_features)

    @Column(name = "duration_days")
    Integer durationDays;

    /** Rate limit (thông số kỹ thuật, admin cấu hình, ẨN khỏi /plans public). null = tuỳ chỉnh/không giới hạn. */
    @Column(name = "rate_limit_per_10s")
    Integer rateLimitPer10s;

    @Column(name = "rate_limit_per_min")
    Integer rateLimitPerMin;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    List<PlanFeature> planFeatures = new ArrayList<>();

    /**
     * IDs of {@link FeatureCatalog} rows enabled for this plan. Stored as plain UUIDs (no FK) so a
     * deleted catalog feature just vanishes from every plan's matrix — the matrix is always rebuilt
     * from the current catalog, and stale ids are ignored.
     */
    @ElementCollection
    @CollectionTable(name = "plan_enabled_features", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "feature_id", nullable = false)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Set<UUID> enabledFeatureIds = new HashSet<>();
}
