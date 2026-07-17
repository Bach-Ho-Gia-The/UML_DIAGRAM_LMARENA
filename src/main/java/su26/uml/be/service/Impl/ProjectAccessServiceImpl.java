package su26.uml.be.service.Impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import su26.uml.be.entity.Project;
import su26.uml.be.entity.User;
import su26.uml.be.exception.AppException;
import su26.uml.be.exception.ErrorCode;
import su26.uml.be.repository.ProjectRepository;
import su26.uml.be.repository.UserRepository;
import su26.uml.be.service.ProjectAccessService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectAccessServiceImpl implements ProjectAccessService {

    ProjectRepository projectRepository;
    UserRepository userRepository;

    @Override
    public Project getProjectAndValidateAccess(UUID projectId, String email) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));
        validateAccess(project, email);
        return project;
    }

    @Override
    public User validateAccess(Project project, String email) {
        if (project == null || project.isDeleted()) {
            throw new AppException(ErrorCode.PROJECT_NOT_FOUND);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean isAdmin = user.getRole().getRoleName().equals("ADMIN");
        boolean isOwner = project.getUser().getEmail().equals(email);

        if (!isAdmin && !isOwner && !Boolean.TRUE.equals(project.getPublicAccess())) {
            throw new AppException(ErrorCode.PROJECT_ACCESS_DENIED);
        }
        return user;
    }
}
