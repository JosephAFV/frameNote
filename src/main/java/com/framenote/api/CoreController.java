package com.framenote.api;
import com.framenote.application.*;
import com.framenote.domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api")
public class CoreController {
 private final WorkspaceService ws;private final ProjectService projects;private final VideoService videos;private final FeedbackService feedback;
 public CoreController(WorkspaceService w,ProjectService p,VideoService v,FeedbackService f,ContentDeletionService deletion){ws=w;projects=p;videos=v;feedback=f;this.deletion=deletion;}
 private final ContentDeletionService deletion;
 private String id(JwtAuthenticationToken jwt){return jwt.getToken().getSubject();}
 public record ProjectRequest(String name){}
 public record MemberRequest(String email,String role){}
 public record StateRequest(String state){}
 public record VideoRequest(String title){}
 public record AnnotationRequest(long timestampMs,Double x,Double y,String text,String geometryJson){}
 public record CommentRequest(String text){}
 public record WorkspaceAccess(String id,String name,String role){}
 public record ProjectAccess(Project project,String role){}
 @GetMapping("/workspace-access") public List<WorkspaceAccess> workspaceAccess(JwtAuthenticationToken a){
   return ws.memberships.findByAccountId(id(a)).stream().map(member -> {
     Workspace space = ws.workspace(member.workspaceId,id(a));
     return new WorkspaceAccess(space.id,space.name,member.role);
   }).toList();
 }
 @GetMapping("/projects/{pid}/access") public ProjectAccess projectAccess(@PathVariable String pid,JwtAuthenticationToken a){
   Project project = ws.project(pid,id(a));
   ProjectMember member = ws.projectMembers.findByProjectIdAndAccountId(pid,id(a)).orElseThrow();
   return new ProjectAccess(project,member.role);
 }
 @GetMapping("/workspaces") public List<Workspace> workspaces(JwtAuthenticationToken a){return ws.workspaces.findByOwnerId(id(a));}
 @GetMapping("/workspaces/{wid}/usage") public Map<String,Object> usage(@PathVariable String wid,JwtAuthenticationToken a){Workspace w=ws.workspace(wid,id(a));return Map.of("plan",w.plan,"usedBytes",w.usedBytes,"maxBytes",w.maxBytes,"maxProjects",w.maxProjects);}
 @PostMapping("/workspaces/{wid}/projects") public Project create(@PathVariable String wid,@RequestBody ProjectRequest r,JwtAuthenticationToken a){return projects.create(wid,id(a),r.name());}
 @GetMapping("/workspaces/{wid}/projects") public List<Project> list(@PathVariable String wid,JwtAuthenticationToken a){return projects.list(wid,id(a));}
 @PostMapping("/projects/{pid}/members") public Project add(@PathVariable String pid,@RequestBody MemberRequest r,JwtAuthenticationToken a){return projects.inviteMember(pid,id(a),r.email(),r.role());}
 @PatchMapping("/projects/{pid}/status") public Project state(@PathVariable String pid,@RequestBody StateRequest r,JwtAuthenticationToken a){return projects.state(pid,id(a),r.state());}
 @PostMapping("/projects/{pid}/videos") public Video createVideo(@PathVariable String pid,@RequestBody VideoRequest r,JwtAuthenticationToken a){return videos.create(pid,id(a),r.title());}
 @GetMapping("/projects/{pid}/videos") public List<Video> listVideos(@PathVariable String pid,JwtAuthenticationToken a){return videos.list(pid,id(a));}
 @PostMapping(value="/videos/{vid}/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public VideoVersion upload(@PathVariable String vid,@RequestParam long durationMs,@RequestParam MultipartFile file,JwtAuthenticationToken a){return videos.upload(vid,id(a),durationMs,file);}
 @GetMapping("/videos/{vid}") public Video videoDetail(@PathVariable String vid,JwtAuthenticationToken a){return videos.video(vid,id(a));}
 @GetMapping("/versions/{vid}") public VideoVersion versionDetail(@PathVariable String vid,JwtAuthenticationToken a){return videos.version(vid,id(a));}
 @GetMapping("/videos/{vid}/versions") public List<VideoVersion> versions(@PathVariable String vid,JwtAuthenticationToken a){return videos.versions(vid,id(a));}
 @GetMapping("/versions/{id}/file") public ResponseEntity<Resource> file(@PathVariable String id,JwtAuthenticationToken a){Resource file=videos.media(id,id(a));return ResponseEntity.ok().contentType(MediaType.parseMediaType("video/mp4")).body(file);}
 @PostMapping("/versions/{vid}/annotations") public Annotation annotate(@PathVariable String vid,@RequestBody AnnotationRequest r,JwtAuthenticationToken a){return feedback.annotate(vid,id(a),r.timestampMs(),r.x(),r.y(),r.text(),r.geometryJson());}
 @GetMapping("/versions/{vid}/annotations") public List<Annotation> annotations(@PathVariable String vid,JwtAuthenticationToken a){return feedback.list(vid,id(a));}
 @PostMapping("/annotations/{aid}/comments") public Comment comment(@PathVariable String aid,@RequestBody CommentRequest r,JwtAuthenticationToken a){return feedback.comment(aid,id(a),r.text());}
 @GetMapping("/annotations/{aid}/comments") public List<Comment> comments(@PathVariable String aid,JwtAuthenticationToken a){return feedback.comments(aid,id(a));}
 @PatchMapping("/annotations/{aid}/status") public Annotation status(@PathVariable String aid,@RequestBody StateRequest r,JwtAuthenticationToken a){return feedback.changeStatus(aid,id(a),r.state());}
 @DeleteMapping("/annotations/{aid}") public Map<String,Boolean> deleteAnnotation(@PathVariable String aid,JwtAuthenticationToken a){
   feedback.delete(aid,id(a));
   return Map.of("deleted",true);
 }
 @DeleteMapping("/projects/{pid}") public Map<String,Boolean> deleteProject(@PathVariable String pid,JwtAuthenticationToken a){deletion.deleteProject(pid,id(a));return Map.of("deleted",true);}
 @DeleteMapping("/videos/{vid}") public Map<String,Boolean> deleteVideo(@PathVariable String vid,JwtAuthenticationToken a){deletion.deleteVideo(vid,id(a));return Map.of("deleted",true);}
 @DeleteMapping("/versions/{vid}") public Map<String,Boolean> deleteVersion(@PathVariable String vid,JwtAuthenticationToken a){deletion.deleteVersion(vid,id(a));return Map.of("deleted",true);}
 @GetMapping("/notifications") public List<Notification> notifications(JwtAuthenticationToken a){return feedback.notifications(id(a));}
 @GetMapping("/projects/{pid}/audit") public List<AuditLog> audit(@PathVariable String pid,JwtAuthenticationToken a){return feedback.audit(pid,id(a));}
}

