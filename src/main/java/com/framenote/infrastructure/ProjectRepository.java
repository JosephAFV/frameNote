package com.framenote.infrastructure;
import com.framenote.domain.Project;
import com.framenote.application.port.ProjectStore;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProjectRepository extends JpaRepository<Project,String>, ProjectStore { @org.springframework.data.jpa.repository.Query("select p.status from Project p where p.id = ?1") java.util.Optional<String> findStatusById(String id); boolean existsByIdAndStatus(String id, String status); java.util.List<Project> findByWorkspaceId(String workspaceId); long countByWorkspaceIdAndStatusNot(String workspaceId,String status); }

