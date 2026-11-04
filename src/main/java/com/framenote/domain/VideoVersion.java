package com.framenote.domain;
import java.time.Instant;
public class VideoVersion {
  public String id = java.util.UUID.randomUUID().toString();
  public String videoId;
  public int version;
  public long durationMs;
  public long sizeBytes;
  public String status;
  public String storageKey;
  public Boolean storagePurged = false;
  public Instant createdAt=Instant.now();
  public VideoVersion() {}
}

