package com.framenote.domain;
import java.time.Instant;
public class Invitation {
  public String id = java.util.UUID.randomUUID().toString();
  public String projectId;
  public String tokenHash;
  public String role;
  public java.time.Instant expiresAt;
  public boolean revoked;
  public Instant createdAt=Instant.now();
  public Invitation() {}
}

