package su26.uml.be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Project extends BaseEntity {

    @Column(name = "project_name", nullable = false)
    String projectName;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User user;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    List<Sheet> sheets = new ArrayList<>();

    @Column(name = "is_deleted")
    @Builder.Default
    boolean isDeleted = false;

    @Column(name = "is_draft")
    @Builder.Default
    boolean isDraft = false;

    @Column(name = "public_access")
    @Builder.Default
    Boolean publicAccess = false;
}
