package com.framenote.domain;
import java.time.Instant;
public class ProjectMember {
  public String id = java.util.UUID.randomUUID().toString();
  public String projectId;
  public String accountId;
  public String role;
  public Instant createdAt=Instant.now();
  public ProjectMember() {}
}

