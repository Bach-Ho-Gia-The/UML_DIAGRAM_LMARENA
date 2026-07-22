package su26.uml.be.features.ai.entity;


import su26.uml.be.common.entity.BaseEntity;import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import su26.uml.be.features.user.entity.User;
import su26.uml.be.features.project.entity.ProjectDiagram;

@Entity
@Table(name = "feedbacks")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Feedback extends BaseEntity {

    @Column(name = "feedback_content", columnDefinition = "TEXT")
    String feedbackContent;

    @Column(name = "rating")
    Integer rating;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User user;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    AnalysisSession session;

    @OneToOne
    @JoinColumn(name = "recommendation_id", unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Recommendation recommendation;

    @OneToOne
    @JoinColumn(name = "project_diagram_id", unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    ProjectDiagram projectDiagram;
}