package su26.uml.be.service;

import su26.uml.be.entity.Project;
import su26.uml.be.entity.User;

import java.util.UUID;

/**
 * Kiểm tra quyền truy cập project dùng chung cho các resource con (sheet, workspace item).
 * Quy tắc (đồng nhất với ProjectServiceImpl): owner ∨ ADMIN ∨ project public.
 */
public interface ProjectAccessService {

    /**
     * Load project theo id, chặn project đã xóa mềm, rồi validate quyền của email.
     */
    Project getProjectAndValidateAccess(UUID projectId, String email);

    /**
     * Validate quyền trên project đã load sẵn (chặn cả project đã xóa mềm).
     */
    User validateAccess(Project project, String email);
}
