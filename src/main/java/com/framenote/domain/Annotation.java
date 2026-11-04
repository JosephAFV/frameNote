package com.framenote.domain;
import java.time.Instant;
public class Annotation {
  public String id = java.util.UUID.randomUUID().toString();
  public String versionId;
  public String authorId;
  public long timestampMs;
  public Double x;
  public Double y;
  public String text;
  public String geometryJson;
  public String status;
  public Instant createdAt=Instant.now();
  public Annotation() {}
}


