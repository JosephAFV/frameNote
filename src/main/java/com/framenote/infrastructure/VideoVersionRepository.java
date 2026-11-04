package com.framenote.infrastructure;
import com.framenote.domain.VideoVersion;
import com.framenote.application.port.VideoVersionStore;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VideoVersionRepository extends JpaRepository<VideoVersion,String>, VideoVersionStore { @org.springframework.data.jpa.repository.Query("select v.videoId from VideoVersion v where v.id = ?1") java.util.Optional<String> findVideoIdById(String id); java.util.List<VideoVersion> findByStatus(String status); boolean existsByIdAndStatus(String id, String status); java.util.List<VideoVersion> findByVideoIdOrderByVersionDesc(String videoId); }

