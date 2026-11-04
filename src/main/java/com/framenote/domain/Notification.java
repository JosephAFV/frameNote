package com.framenote.domain;
import java.time.Instant;
public class Notification {
  public String id = java.util.UUID.randomUUID().toString();
  public String recipientId;
  public String message;
  public boolean readFlag;
  public Instant createdAt=Instant.now();
  public Notification() {}
}

