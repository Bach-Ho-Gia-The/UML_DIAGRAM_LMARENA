package su26.uml.be.dto.response;

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
public class AiEdgeDto {
    String id;
    String source;
    String target;
    String relation;
    String label;
    String multiplicitySource;
    String multiplicityTarget;
}
