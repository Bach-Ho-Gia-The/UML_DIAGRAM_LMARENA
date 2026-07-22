package su26.uml.be.infrastructure.socket.dto;

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