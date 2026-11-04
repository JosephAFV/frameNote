package com.framenote.infrastructure;

import com.framenote.application.port.*;
import com.framenote.domain.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.UncheckedIOException;

@Configuration
@EnableScheduling
public class DeletedMediaCleanup {
    private static final Logger log = LoggerFactory.getLogger(DeletedMediaCleanup.class);
    private final VideoVersionStore versions;
    private final VideoStore videos;
    private final ProjectStore projects;
    private final WorkspaceStore spaces;
    private final MediaRemoval media;
    private final TransactionTemplate transactions;

    public DeletedMediaCleanup(VideoVersionStore versions, VideoStore videos, ProjectStore projects,
                               WorkspaceStore spaces, MediaRemoval media, TransactionTemplate transactions) {
        this.versions = versions;
        this.videos = videos;
        this.projects = projects;
        this.spaces = spaces;
        this.media = media;
        this.transactions = transactions;
    }

    @Scheduled(fixedDelay = 3000)
    public void clean() {
        for (VideoVersion candidate : versions.findByStatus("DELETED")) {
            if (Boolean.TRUE.equals(candidate.storagePurged)) continue;
            try {
                Video video = videos.findById(candidate.videoId).orElseThrow();
                Project project = projects.findById(video.projectId).orElseThrow();
                transactions.executeWithoutResult(status -> {
                    Workspace space = spaces.lockById(project.workspaceId).orElseThrow();
                    VideoVersion version = versions.findById(candidate.id).orElseThrow();
                    if (Boolean.TRUE.equals(version.storagePurged)) return;
                    try {
                        media.delete(version.storageKey);
                    } catch (IOException error) {
                        throw new UncheckedIOException(error);
                    }
                    space.usedBytes = Math.max(0, space.usedBytes - version.sizeBytes);
                    version.storagePurged = true;
                    spaces.save(space);
                    versions.save(version);
                });
            } catch (RuntimeException error) {
                log.warn("Se reintentará limpiar la versión {}", candidate.id, error);
            }
        }
    }
}
