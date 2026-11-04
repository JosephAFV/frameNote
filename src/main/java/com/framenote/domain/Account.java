package com.framenote.domain;
import java.time.Instant;
public class Account {
  public String id = java.util.UUID.randomUUID().toString();
  public String email;
  public String passwordHash;
  public String name;
  public Instant createdAt=Instant.now();
  public Account() {}
}

