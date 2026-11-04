package com.framenote.infrastructure;
import com.framenote.domain.Video;
import com.framenote.application.port.VideoStore;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VideoRepository extends JpaRepository<Video,String>, VideoStore { boolean existsByIdAndStatus(String id, String status); java.util.List<Video> findByProjectId(String projectId); }

