package com.framenote.application.port;

import com.framenote.domain.Comment;

public interface CommentStore {
    java.util.Optional<Comment> findById(String id);
    <S extends Comment> S save(S entity);
    java.util.List<Comment> findByAnnotationIdOrderByCreatedAtAsc(String annotationId);
}
