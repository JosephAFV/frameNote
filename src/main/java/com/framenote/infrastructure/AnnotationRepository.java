package com.framenote.infrastructure;
import com.framenote.domain.Annotation;
import com.framenote.application.port.AnnotationStore;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AnnotationRepository extends JpaRepository<Annotation,String>, AnnotationStore { java.util.List<Annotation> findByVersionIdOrderByTimestampMsAsc(String versionId); }

