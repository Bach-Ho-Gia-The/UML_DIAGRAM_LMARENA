package su26.uml.be.infrastructure.socket;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import su26.uml.be.infrastructure.socket.dto.CanvasEvent;
import su26.uml.be.infrastructure.socket.dto.CursorEvent;
import su26.uml.be.infrastructure.socket.dto.SelectionEvent;

@Slf4j
@Component
public class SocketModule {

    private final SocketIOServer server;

    public SocketModule(SocketIOServer server) {
        this.server = server;
        this.server.addConnectListener(onConnected());
        this.server.addDisconnectListener(onDisconnected());
        
        // Sá»± kiá»?n join room
        this.server.addEventListener("room:join", String.class, onJoinRoom());
        
        // Sá»± kiá»?n leave room
        this.server.addEventListener("room:leave", String.class, onLeaveRoom());
        
        // Sá»± kiá»?n cursor move
        this.server.addEventListener("cursor:move", CursorEvent.class, onCursorMove());
        
        // Sá»± kiá»?n selection change
        this.server.addEventListener("selection:change", SelectionEvent.class, onSelectionChange());
        
        // Sá»± kiá»?n canvas change
        this.server.addEventListener("canvas:change", CanvasEvent.class, onCanvasChange());
    }

    private ConnectListener onConnected() {
        return client -> {
            log.info("Client connected: {}", client.getSessionId());
        };
    }

    private DisconnectListener onDisconnected() {
        return client -> {
            log.info("Client disconnected: {}", client.getSessionId());
        };
    }

    private DataListener<String> onJoinRoom() {
        return (client, sheetId, ackSender) -> {
            log.info("Client {} joining room: {}", client.getSessionId(), sheetId);
            client.joinRoom(sheetId);
        };
    }

    private DataListener<String> onLeaveRoom() {
        return (client, sheetId, ackSender) -> {
            log.info("Client {} leaving room: {}", client.getSessionId(), sheetId);
            client.leaveRoom(sheetId);
        };
    }

    private DataListener<CursorEvent> onCursorMove() {
        return (client, data, ackSender) -> {
            String sheetId = data.getSheetId();
            // Broadcast cho táº¥t cáº£ nhá»¯ng ngÆ°á»i khÃ¡c trong cÃ¹ng room
            server.getRoomOperations(sheetId).sendEvent("cursor:update", client, data);
        };
    }

    private DataListener<SelectionEvent> onSelectionChange() {
        return (client, data, ackSender) -> {
            String sheetId = data.getSheetId();
            // Broadcast cho táº¥t cáº£ nhá»¯ng ngÆ°á»i khÃ¡c trong cÃ¹ng room
            server.getRoomOperations(sheetId).sendEvent("selection:update", client, data);
        };
    }

    private DataListener<CanvasEvent> onCanvasChange() {
        return (client, data, ackSender) -> {
            String sheetId = data.getSheetId();
            // Broadcast cho táº¥t cáº£ nhá»¯ng ngÆ°á»i khÃ¡c trong cÃ¹ng room
            server.getRoomOperations(sheetId).sendEvent("canvas:update", client, data);
        };
    }
}
