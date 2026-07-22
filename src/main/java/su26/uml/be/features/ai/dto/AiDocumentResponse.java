package su26.uml.be.features.ai.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiDocumentResponse {
    String docId;
    String filename;
    String docpath;
    Long size;
    String status;
    String uploadedAt;
}