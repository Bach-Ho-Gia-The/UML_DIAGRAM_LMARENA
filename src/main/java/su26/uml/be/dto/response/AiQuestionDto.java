package su26.uml.be.dto.response;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiQuestionDto {
    String id;
    String edgeId;
    String prompt;
    String detail;
    String mode; // "single" or "multiple"
    List<AiOptionDto> options;
}
