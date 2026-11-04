package com.framenote.infrastructure;
import com.framenote.domain.AuditLog;
import com.framenote.application.port.AuditLogStore;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditLogRepository extends JpaRepository<AuditLog,String>, AuditLogStore { java.util.List<AuditLog> findByProjectIdOrderByCreatedAtDesc(String projectId); }

