package su26.uml.be.features.user.repository;

import su26.uml.be.features.user.entity.User;
import su26.uml.be.common.constant.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findAllByStatusAndDeletionDateLessThanEqual(UserStatus status, LocalDateTime now);
    long countByCreatedAtBetweenAndStatusAndRoleRoleName(LocalDateTime from, LocalDateTime to, UserStatus status, String roleName);
    long countByStatusAndRoleRoleName(UserStatus status, String roleName);
    long countByLastActiveAtAfterAndStatusAndRoleRoleName(LocalDateTime after, UserStatus status, String roleName);
}