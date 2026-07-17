package su26.uml.be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "exam_uploads")
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamUpload extends BaseEntity {

    @Column(name = "file_name", nullable = false)
    String fileName;

    @Column(name = "file_url", columnDefinition = "TEXT", nullable = false)
    String fileUrl;

    @Column(name = "file_type")
    String fileType;

    @Column(name = "upload_status")
    String uploadStatus;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Project project;

    @ManyToOne
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User uploadedBy;
}
