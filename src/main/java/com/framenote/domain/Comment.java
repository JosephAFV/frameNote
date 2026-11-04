package com.framenote.domain;
import java.time.Instant;
public class Comment {
  public String id = java.util.UUID.randomUUID().toString();
  public String annotationId;
  public String authorId;
  public String text;
  public Instant createdAt=Instant.now();
  public Comment() {}
}

