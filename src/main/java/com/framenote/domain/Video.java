package com.framenote.domain;
import java.time.Instant;
public class Video {
  public String id = java.util.UUID.randomUUID().toString();
  public String projectId;
  public String title;
  public String status;
  public Instant createdAt=Instant.now();
  public Video() {}
}

