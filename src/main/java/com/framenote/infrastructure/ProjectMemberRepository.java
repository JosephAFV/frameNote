package com.framenote.infrastructure;
import com.framenote.domain.ProjectMember;
import com.framenote.application.port.ProjectMemberStore;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProjectMemberRepository extends JpaRepository<ProjectMember,String>, ProjectMemberStore { java.util.Optional<ProjectMember> findByProjectIdAndAccountId(String projectId,String accountId); java.util.List<ProjectMember> findByProjectId(String projectId); }

