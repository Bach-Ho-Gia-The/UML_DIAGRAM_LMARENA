package su26.uml.be.infrastructure.socket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorEvent {
    private String sheetId;
    private String username;
    private String color;
    private double x;
    private double y;
}