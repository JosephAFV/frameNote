package com.framenote.domain;
import java.time.Instant;
public class Workspace {
  public String id = java.util.UUID.randomUUID().toString();
  public String ownerId;
  public String name;
  public String plan;
  public long maxBytes;
  public long usedBytes;
  public long reservedBytes;
  public int maxProjects;
  public Instant createdAt=Instant.now();
  public Workspace() {}
}

