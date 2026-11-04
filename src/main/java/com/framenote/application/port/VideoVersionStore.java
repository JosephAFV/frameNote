package com.framenote.application.port;

import com.framenote.domain.VideoVersion;

public interface VideoVersionStore {
    java.util.Optional<VideoVersion> findById(String id);
    boolean existsByIdAndStatus(String id, String status);
    <S extends VideoVersion> S save(S entity);
    java.util.Optional<String> findVideoIdById(String id);
    java.util.List<VideoVersion> findByStatus(String status);
    java.util.List<VideoVersion> findByVideoIdOrderByVersionDesc(String videoId);
}
