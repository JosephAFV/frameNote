package com.framenote.application;
import com.framenote.domain.BusinessRules;
import com.framenote.domain.*;
import com.framenote.application.port.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class FeedbackService {
 private final WorkspaceService auth; private final VideoService videos;private final AnnotationStore annotations;private final CommentStore comments;private final VideoStore vids;private final NotificationStore notices;private final AuditLogStore logs;
 public FeedbackService(WorkspaceService a,VideoService v,AnnotationStore an,CommentStore c,VideoStore vs,NotificationStore n,AuditLogStore l){auth=a;videos=v;annotations=an;comments=c;vids=vs;notices=n;logs=l;}
 private String projectId(String versionId,String actor){VideoVersion version=videos.version(versionId,actor);return vids.findById(version.videoId).orElseThrow().projectId;}
 private void audit(String project,String actor,String action){AuditLog log=new AuditLog();log.projectId=project;log.actorId=actor;log.action=action;logs.save(log);}
 @Transactional public Annotation annotate(String versionId,String actor,long time,Double x,Double y,String text,String geometryJson){VideoVersion vv=videos.version(versionId,actor);String pid=projectId(versionId,actor);auth.allowProject(pid,actor,"OWNER","EDITOR","REVIEWER");BusinessRules.requireEditable(auth.project(pid,actor).status);
    videos.requireCurrentVersion(vv,actor);
    if(!"AVAILABLE".equals(vv.status))throw new IllegalArgumentException("Versión no disponible");
    BusinessRules.validTimestamp(time,vv.durationMs);BusinessRules.validCoordinates(x,y);
    if((text==null||text.isBlank())&&(geometryJson==null||geometryJson.isBlank()))throw new IllegalArgumentException("Incluye texto o geometría");
    if(text!=null&&text.length()>2000)throw new IllegalArgumentException("Comentario demasiado largo");
    Annotation a=new Annotation();a.versionId=versionId;a.authorId=actor;a.timestampMs=time;a.x=x;a.y=y;a.text=text;a.geometryJson=geometryJson;a.status="PENDING";annotations.save(a);
    for(ProjectMember m:auth.projectMembers.findByProjectId(pid))if(m.role.equals("EDITOR")&&!m.accountId.equals(actor)){
      Notification no=new Notification();no.recipientId=m.accountId;no.message="Nueva observación en el segundo "+(time/1000);notices.save(no);
    }
    audit(pid,actor,"ANNOTATION_CREATED:"+a.id);return a;
 }
 public List<Annotation> list(String versionId,String actor){projectId(versionId,actor);return annotations.findByVersionIdOrderByTimestampMsAsc(versionId).stream().filter(a -> !"DELETED".equals(a.status)).toList();}
 public Annotation annotation(String id,String actor){Annotation a=annotations.findById(id).orElseThrow();projectId(a.versionId,actor);if ("DELETED".equals(a.status)) throw new NoSuchElementException();return a;}
 @Transactional public Comment comment(String id,String actor,String text){Annotation a=annotation(id,actor);String pid=projectId(a.versionId,actor);auth.allowProject(pid,actor,"OWNER","EDITOR","REVIEWER");BusinessRules.requireEditable(auth.project(pid,actor).status);
   videos.requireCurrentVersion(videos.version(a.versionId,actor),actor);
   if(text==null||text.isBlank()||text.length()>2000)throw new IllegalArgumentException("Texto inválido");Comment c=new Comment();c.annotationId=id;c.authorId=actor;c.text=text.strip();comments.save(c);audit(pid,actor,"COMMENT_CREATED:"+c.id);return c;
 }
 public List<Comment> comments(String id,String actor){annotation(id,actor);return comments.findByAnnotationIdOrderByCreatedAtAsc(id);}
 @Transactional public Annotation changeStatus(String id,String actor,String next){Annotation a=annotation(id,actor);String pid=projectId(a.versionId,actor);auth.allowProject(pid,actor,"OWNER","EDITOR");BusinessRules.requireEditable(auth.project(pid,actor).status);
   videos.requireCurrentVersion(videos.version(a.versionId,actor),actor);
   BusinessRules.validFeedbackTransition(a.status,next);a.status=next;annotations.save(a);audit(pid,actor,"ANNOTATION_"+next+":"+a.id);return a;
 }
 @Transactional public void delete(String id,String actor){
   Annotation a=annotation(id,actor);
   String pid=projectId(a.versionId,actor);
   if (!actor.equals(a.authorId)) auth.allowProject(pid,actor,"OWNER");
   else auth.allowProject(pid,actor,"OWNER","EDITOR","REVIEWER");
   BusinessRules.requireEditable(auth.project(pid,actor).status);
   videos.requireCurrentVersion(videos.version(a.versionId,actor),actor);
   a.status="DELETED";
   audit(pid,actor,"ANNOTATION_DELETED:"+id);
 }
 public List<Notification> notifications(String actor){return notices.findByRecipientIdOrderByCreatedAtDesc(actor);}
 public List<AuditLog> audit(String pid,String actor){auth.allowProject(pid,actor,"OWNER");return logs.findByProjectIdOrderByCreatedAtDesc(pid);}
}

