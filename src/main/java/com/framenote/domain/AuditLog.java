package com.framenote.domain;
import java.time.Instant;
public class AuditLog {
  public String id = java.util.UUID.randomUUID().toString();
  public String actorId;
  public String projectId;
  public String action;
  public Instant createdAt=Instant.now();
  public AuditLog() {}
}

