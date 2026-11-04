package com.framenote.application;
import com.framenote.domain.BusinessRules;
import com.framenote.domain.*;
import com.framenote.application.port.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.*;
@Service
public class VideoService {
 private final WorkspaceService permissions;private final VideoStore videos;private final VideoVersionStore versions;private final WorkspaceStore workspaces;
 private final Path location=Paths.get(System.getenv().getOrDefault("MEDIA_DIR","./media")).toAbsolutePath().normalize();
 public VideoService(WorkspaceService p,VideoStore v,VideoVersionStore vv,WorkspaceStore w){permissions=p;videos=v;versions=vv;workspaces=w;}
 @Transactional public Video create(String projectId,String user,String title){permissions.allowProject(projectId,user,"OWNER","EDITOR");Project p=permissions.project(projectId,user);BusinessRules.requireEditable(p.status);
  BusinessRules.requireEditable(permissions.lockProject(projectId,user).status);
  if(title==null||title.isBlank()||title.length()>120)throw new IllegalArgumentException("Título inválido");Video v=new Video();v.projectId=projectId;v.title=title.strip();return videos.save(v);}
 public List<Video> list(String pid,String user){permissions.project(pid,user);return videos.findByProjectId(pid).stream().filter(v -> !"DELETED".equals(v.status)).toList();}
 public VideoVersion version(String id,String user){VideoVersion v=versions.findById(id).orElseThrow();video(v.videoId,user);if(versions.existsByIdAndStatus(id,"DELETED"))throw new NoSuchElementException();return v;}
 public void requireCurrentVersion(VideoVersion version, String actor) {
   Video video = video(version.videoId, actor);
   Project project = permissions.lockProject(video.projectId, actor);
   BusinessRules.requireEditable(project.status);
   List<VideoVersion> history = versions.findByVideoIdOrderByVersionDesc(video.id).stream().filter(v -> !"DELETED".equals(v.status)).toList();
   version(version.id, actor);
   if (history.isEmpty() || !history.getFirst().id.equals(version.id)) {
     throw new IllegalArgumentException("La versión anterior es de solo lectura");
   }
 }
 public Video video(String id,String user){Video v=videos.findById(id).orElseThrow();permissions.project(v.projectId,user);if(videos.existsByIdAndStatus(id,"DELETED"))throw new NoSuchElementException();return v;}
 @Transactional public VideoVersion upload(String videoId,String user,long durationMs,MultipartFile file){Video v=video(videoId,user);permissions.allowProject(v.projectId,user,"OWNER","EDITOR");Project p=permissions.project(v.projectId,user);BusinessRules.requireEditable(p.status);
   if(file==null||file.isEmpty()||file.getSize()<=0||durationMs<=0)throw new IllegalArgumentException("Archivo o duración inválidos");
   Workspace w=workspaces.lockById(p.workspaceId).orElseThrow(); video(videoId,user); BusinessRules.requireEditable(permissions.lockProject(v.projectId,user).status); BusinessRules.validQuota(w.usedBytes,w.reservedBytes,file.getSize(),w.maxBytes);
   if(file.getSize()>250L*1024*1024)throw new IllegalArgumentException("Archivo demasiado grande para este MVP");
   try(InputStream in=file.getInputStream()){byte[] header=in.readNBytes(12);if(header.length<12||header[4]!='f'||header[5]!='t'||header[6]!='y'||header[7]!='p')throw new IllegalArgumentException("Solo archivos MP4 válidos");}
   catch(IOException e){throw new IllegalArgumentException("No se pudo leer el archivo");}
   int n=versions.findByVideoIdOrderByVersionDesc(videoId).stream().mapToInt(x->x.version).max().orElse(0)+1;
   VideoVersion vv=new VideoVersion();vv.videoId=videoId;vv.version=n;vv.durationMs=durationMs;vv.sizeBytes=file.getSize();vv.status="AVAILABLE";
   try{Files.createDirectories(location);String key=UUID.randomUUID()+".mp4";Path target=location.resolve(key).normalize();if(!target.startsWith(location))throw new IOException("Ruta no válida");file.transferTo(target);vv.storageKey=key;versions.save(vv);w.usedBytes+=file.getSize();workspaces.save(w);return vv;}
   catch(IOException e){throw new IllegalArgumentException("Fallo al guardar video: "+e.getMessage());}
 }
 public List<VideoVersion> versions(String videoId,String user){video(videoId,user);return versions.findByVideoIdOrderByVersionDesc(videoId).stream().filter(v -> !"DELETED".equals(v.status)).toList();}
 public org.springframework.core.io.Resource media(String versionId,String user){VideoVersion v=version(versionId,user);if(!"AVAILABLE".equals(v.status))throw new IllegalArgumentException("Video no disponible");Path file=location.resolve(v.storageKey).normalize();if(!file.startsWith(location))throw new IllegalArgumentException("Ruta inválida");return new org.springframework.core.io.FileSystemResource(file);}
}

