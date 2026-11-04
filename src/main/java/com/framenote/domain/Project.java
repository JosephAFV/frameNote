package com.framenote.domain;
import java.time.Instant;
public class Project {
  public String id = java.util.UUID.randomUUID().toString();
  public String workspaceId;
  public String name;
  public String status;
  public Instant createdAt=Instant.now();
  public Project() {}
}

