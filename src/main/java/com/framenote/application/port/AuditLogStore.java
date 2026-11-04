package com.framenote.application.port;

import com.framenote.domain.AuditLog;

public interface AuditLogStore {
    java.util.Optional<AuditLog> findById(String id);
    <S extends AuditLog> S save(S entity);
    java.util.List<AuditLog> findByProjectIdOrderByCreatedAtDesc(String projectId);
}
