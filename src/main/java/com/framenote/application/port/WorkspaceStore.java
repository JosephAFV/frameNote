package com.framenote.application.port;

import com.framenote.domain.Workspace;

public interface WorkspaceStore {
    java.util.Optional<Workspace> findById(String id);
    <S extends Workspace> S save(S entity);
    java.util.List<Workspace> findByOwnerId(String ownerId);
    java.util.Optional<Workspace> lockById(String id);
}
