package su26.uml.be.dto.socket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectionEvent {
    private String sheetId;
    private List<String> nodeIds;
    private List<String> edgeIds;
}
