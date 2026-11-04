package com.framenote.application.port;

import com.framenote.domain.Video;

public interface VideoStore {
    java.util.Optional<Video> findById(String id);
    boolean existsByIdAndStatus(String id, String status);
    <S extends Video> S save(S entity);
    java.util.List<Video> findByProjectId(String projectId);
}
