package su26.uml.be.dto.projection;

import java.util.UUID;

/**
 * Aggregate row for "Top Projects": one project with its diagram (sheet) count.
 * Populated by {@code SheetRepository.findTopProjects(...)}.
 */
public interface TopProjectProjection {
    UUID getProjectId();
    String getProjectName();
    String getOwnerEmail();
    long getDiagramCount();
}
