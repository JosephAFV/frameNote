package com.framenote.infrastructure;
import com.framenote.domain.Comment;
import com.framenote.application.port.CommentStore;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CommentRepository extends JpaRepository<Comment,String>, CommentStore { java.util.List<Comment> findByAnnotationIdOrderByCreatedAtAsc(String annotationId); }

