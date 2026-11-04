package com.framenote.application.port;

import com.framenote.domain.Project;

public interface ProjectStore {
    java.util.Optional<Project> findById(String id);
    java.util.Optional<String> findStatusById(String id);
    boolean existsByIdAndStatus(String id, String status);
    <S extends Project> S save(S entity);
    java.util.List<Project> findByWorkspaceId(String workspaceId);
    long countByWorkspaceIdAndStatusNot(String workspaceId,String status);
}
