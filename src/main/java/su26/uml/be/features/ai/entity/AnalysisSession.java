package su26.uml.be.features.ai.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.features.project.entity.Project;

@Entity
@Table(name = "analysis_sessions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnalysisSession extends BaseEntity {

    @Column(name = "session_name")
    String sessionName;

    @Column(name = "status")
    String status;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Project project;

    @ManyToOne
    @JoinColumn(name = "exam_upload_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    ExamUpload examUpload;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User user;
}