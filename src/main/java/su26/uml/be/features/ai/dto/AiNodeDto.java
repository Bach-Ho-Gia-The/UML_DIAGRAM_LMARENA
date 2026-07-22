package su26.uml.be.features.ai.dto;

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
public class AiNodeDto {
    String id;
    String type;
    String label;
    String stereotype;
    List<String> attributes;
    List<String> methods;
    String parentId;
}