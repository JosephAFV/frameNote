package com.framenote.application.port;

import com.framenote.domain.Annotation;

public interface AnnotationStore {
    java.util.Optional<Annotation> findById(String id);
    <S extends Annotation> S save(S entity);
    java.util.List<Annotation> findByVersionIdOrderByTimestampMsAsc(String versionId);
}
