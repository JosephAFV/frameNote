package com.framenote.domain;
import java.time.Instant;
public class Membership {
  public String id = java.util.UUID.randomUUID().toString();
  public String workspaceId;
  public String accountId;
  public String role;
  public Instant createdAt=Instant.now();
  public Membership() {}
}

