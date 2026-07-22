package su26.uml.be.features.diagram.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import su26.uml.be.common.constant.enums.DiagramVersionSource;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.features.project.entity.Sheet;

@Entity
@Table(name = "diagram_versions",
        uniqueConstraints = @UniqueConstraint(name = "uq_diagram_versions_sheet_number",
                columnNames = {"sheet_id", "version_number"}),
        indexes = @Index(name = "idx_diagram_versions_sheet", columnList = "sheet_id"))
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiagramVersion extends BaseEntity {

    @Column(name = "version_number", nullable = false)
    Long versionNumber;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    DiagramVersionSource source;

    // Snapshot đầy đủ (nodes/edges/diagramType/viewport) — immutable sau khi tạo
    @Column(name = "diagram_data", columnDefinition = "TEXT", nullable = false)
    String diagramData;

    // SHA-256 của diagramData — dedupe checkpoint AUTO trùng nội dung
    @Column(name = "content_hash", nullable = false, length = 128)
    String contentHash;

    @Column(name = "schema_version", nullable = false)
    Integer schemaVersion;

    @ManyToOne
    @JoinColumn(name = "sheet_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Sheet sheet;

    @ManyToOne
    @JoinColumn(name = "created_by")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User createdBy;

    // Chỉ set trên bản ghi RESTORE — trỏ về version đã được khôi phục
    @ManyToOne
    @JoinColumn(name = "restored_from_version_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    DiagramVersion restoredFromVersion;
}