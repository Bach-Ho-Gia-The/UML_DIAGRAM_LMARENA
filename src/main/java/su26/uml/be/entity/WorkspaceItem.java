package su26.uml.be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import su26.uml.be.enums.WorkspaceItemKind;

@Entity
@Table(name = "workspace_items", indexes = {
        @Index(name = "idx_workspace_items_project", columnList = "project_id"),
        @Index(name = "idx_workspace_items_parent", columnList = "parent_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkspaceItem extends BaseEntity {

    @Column(name = "name", nullable = false)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    WorkspaceItemKind kind;

    @Column(name = "order_index", nullable = false)
    Integer orderIndex;

    // Chỉ dùng cho kind = MARKDOWN (ràng buộc enforce ở service — ddl-auto không tạo được CHECK)
    @Column(name = "markdown_content", columnDefinition = "TEXT")
    String markdownContent;

    @Version
    @Column(name = "version", nullable = false)
    Long version;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Project project;

    // null = nằm ở gốc project; chỉ FOLDER mới được làm parent
    @ManyToOne
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    WorkspaceItem parent;

    // Bắt buộc với kind = DIAGRAM; unique → backfill chạy lại không tạo item trùng
    @OneToOne
    @JoinColumn(name = "sheet_id", unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Sheet sheet;

    @ManyToOne
    @JoinColumn(name = "created_by")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User createdBy;
}
