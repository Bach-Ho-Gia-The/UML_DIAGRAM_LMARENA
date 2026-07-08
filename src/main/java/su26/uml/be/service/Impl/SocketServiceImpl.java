package su26.uml.be.service.Impl;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import su26.uml.be.entity.Project;
import su26.uml.be.entity.Sheet;
import su26.uml.be.repository.ProjectRepository;
import su26.uml.be.service.SocketService;

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
}
