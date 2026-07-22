package su26.uml.be.infrastructure.socket;

import java.util.UUID;

public interface SocketService {
    void broadcastCollabDisabled(UUID projectId);

    /**
     * Phát "workspace:update" tới room {@code project:{projectId}} để collaborator refresh
     * cây file. Phát SAU KHI transaction hiện tại commit (đăng ký afterCommit); nếu gọi ngoài
     * transaction thì phát ngay.
     */
    void broadcastWorkspaceChanged(UUID projectId);
}