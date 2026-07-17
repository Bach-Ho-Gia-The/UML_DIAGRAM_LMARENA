package su26.uml.be.service.Impl;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import su26.uml.be.entity.Sheet;
import su26.uml.be.repository.ProjectRepository;
import su26.uml.be.service.SocketService;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SocketServiceImpl implements SocketService {

    SocketIOServer server;
    ProjectRepository projectRepository;

    @Override
    public void broadcastCollabDisabled(UUID projectId) {
        projectRepository.findById(projectId).ifPresent(project -> {
            for (Sheet sheet : project.getSheets()) {
                String roomName = sheet.getId().toString();
                log.info("Broadcasting collab:disabled to room: {}", roomName);
                server.getRoomOperations(roomName).sendEvent("collab:disabled", "Project is now private");
            }
        });
    }

    @Override
    public void broadcastWorkspaceChanged(UUID projectId) {
        // Không broadcast trước khi DB commit — collaborator refresh sẽ thấy dữ liệu cũ
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emitWorkspaceChanged(projectId);
                }
            });
        } else {
            emitWorkspaceChanged(projectId);
        }
    }

    private void emitWorkspaceChanged(UUID projectId) {
        String roomName = "project:" + projectId;
        log.debug("Broadcasting workspace:update to room: {}", roomName);
        server.getRoomOperations(roomName).sendEvent("workspace:update", Map.of("projectId", projectId.toString()));
    }
}
