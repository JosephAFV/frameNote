package com.framenote.application;

import com.framenote.application.port.*;
import com.framenote.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentDeletionService {
    private final WorkspaceService access;
    private final VideoStore videos;
    private final VideoVersionStore versions;
    private final AuditLogStore audit;

    public ContentDeletionService(WorkspaceService access, VideoStore videos,
                                  VideoVersionStore versions, AuditLogStore audit) {
        this.access = access;
        this.videos = videos;
        this.versions = versions;
        this.audit = audit;
    }

    private Project authorize(String projectId, String actor, String... roles) {
        Project project = access.lockProject(projectId, actor);
        access.allowProject(projectId, actor, roles);
        BusinessRules.requireEditable(project.status);
        return project;
    }

    @Transactional
    public void deleteProject(String id, String actor) {
        Project project = authorize(id, actor, "OWNER");
        for (Video video : videos.findByProjectId(id)) removeVideo(video);
        project.status = "DELETED";
        access.projects.save(project);
        record(id, actor, "PROJECT_DELETED:" + id);
    }

    @Transactional
    public void deleteVideo(String id, String actor) {
        Video video = videos.findById(id).orElseThrow();
        authorize(video.projectId, actor, "OWNER", "EDITOR");
        removeVideo(video);
        record(video.projectId, actor, "VIDEO_DELETED:" + id);
    }

    @Transactional
    public void deleteVersion(String id, String actor) {
        String videoId = versions.findVideoIdById(id).orElseThrow();
        Video video = videos.findById(videoId).orElseThrow();
        authorize(video.projectId, actor, "OWNER", "EDITOR");
        VideoVersion version = versions.findById(id).orElseThrow();
        version.status = "DELETED";
        versions.save(version);
        record(video.projectId, actor, "VERSION_DELETED:" + id);
    }

    private void removeVideo(Video video) {
        for (VideoVersion version : versions.findByVideoIdOrderByVersionDesc(video.id)) {
            version.status = "DELETED";
            versions.save(version);
        }
        video.status = "DELETED";
        videos.save(video);
    }

    private void record(String projectId, String actor, String action) {
        AuditLog log = new AuditLog();
        log.projectId = projectId;
        log.actorId = actor;
        log.action = action;
        audit.save(log);
    }
}
