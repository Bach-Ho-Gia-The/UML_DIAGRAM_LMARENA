package su26.uml.be.infrastructure.socket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanvasEvent {
    private String sheetId;
    private String senderId; // ID của người gửi để tránh broadcast lại cho chính họ
    private String type; // move, add, remove, update, layout
    private List<Map<String, Object>> nodes;
    private List<Map<String, Object>> edges;
}