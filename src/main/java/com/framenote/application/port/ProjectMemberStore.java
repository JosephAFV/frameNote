package com.framenote.application.port;

import com.framenote.domain.ProjectMember;

public interface ProjectMemberStore {
    java.util.Optional<ProjectMember> findById(String id);
    <S extends ProjectMember> S save(S entity);
    java.util.Optional<ProjectMember> findByProjectIdAndAccountId(String projectId,String accountId);
    java.util.List<ProjectMember> findByProjectId(String projectId);
}
