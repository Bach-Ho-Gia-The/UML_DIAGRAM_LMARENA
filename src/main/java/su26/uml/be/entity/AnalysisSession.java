package su26.uml.be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

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
