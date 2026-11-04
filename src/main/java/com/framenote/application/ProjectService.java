package com.framenote.application;
import com.framenote.domain.BusinessRules;
import com.framenote.domain.*;
import com.framenote.application.port.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class ProjectService {
 private final WorkspaceService auth;private final WorkspaceStore spaces;private final ProjectStore projects;private final ProjectMemberStore members;private final AccountStore accounts;private final VideoStore videos;
 public ProjectService(WorkspaceService a,WorkspaceStore s,ProjectStore p,ProjectMemberStore m,AccountStore ac,VideoStore v){auth=a;spaces=s;projects=p;members=m;accounts=ac;videos=v;}
 @Transactional public Project create(String wid,String user,String name){auth.owner(wid,user);spaces.lockById(wid).orElseThrow();Workspace w=spaces.findById(wid).orElseThrow();
   if(name==null||name.isBlank()||name.length()>120)throw new IllegalArgumentException("Nombre inválido");
   if(projects.findByWorkspaceId(wid).stream().filter(p -> !List.of("ARCHIVED","DELETED").contains(p.status)).count()>=w.maxProjects)throw new IllegalArgumentException("Límite de proyectos Free alcanzado");
   Project p=new Project();p.workspaceId=wid;p.name=name.strip();p.status="DRAFT";projects.save(p);
   ProjectMember pm=new ProjectMember();pm.projectId=p.id;pm.accountId=user;pm.role="OWNER";members.save(pm);return p;
 }
 public List<Project> list(String wid,String user){auth.workspace(wid,user);return projects.findByWorkspaceId(wid).stream().filter(p->!"DELETED".equals(p.status) && auth.isProjectMember(p.id,user)).toList();}
 @Transactional public Project inviteMember(String pid,String actor,String email,String role){auth.allowProject(pid,actor,"OWNER");Project p=auth.lockProject(pid,actor);
   if(!List.of("EDITOR","REVIEWER").contains(role))throw new IllegalArgumentException("Rol no permitido");Account a=accounts.findByEmail(email.strip().toLowerCase()).orElseThrow(()->new IllegalArgumentException("Invitado debe tener cuenta"));
   if(members.findByProjectIdAndAccountId(pid,a.id).isPresent())throw new IllegalArgumentException("Ya participa");
   // El usuario debe pertenecer al espacio para consultar el proyecto.
   if(auth.memberships.findByWorkspaceIdAndAccountId(p.workspaceId,a.id).isEmpty()){
      Membership ms=new Membership();ms.workspaceId=p.workspaceId;ms.accountId=a.id;ms.role="MEMBER";auth.memberships.save(ms);
   }
   ProjectMember pm=new ProjectMember();pm.projectId=pid;pm.accountId=a.id;pm.role=role;members.save(pm);return p;
 }
 @Transactional public Project state(String pid,String actor,String next){Project p=auth.lockProject(pid,actor);
   switch(next){case "EDITING" -> {auth.allowProject(pid,actor,"OWNER","EDITOR");if(!List.of("DRAFT","CHANGES_REQUESTED").contains(p.status))throw new IllegalArgumentException("Cambio no permitido");}
    case "IN_REVIEW" -> {auth.allowProject(pid,actor,"OWNER","EDITOR");if(!"EDITING".equals(p.status))throw new IllegalArgumentException("Cambio no permitido");}
    case "CHANGES_REQUESTED" -> {auth.allowProject(pid,actor,"OWNER","REVIEWER");if(!"IN_REVIEW".equals(p.status))throw new IllegalArgumentException("Cambio no permitido");}
    case "APPROVED" -> {auth.allowProject(pid,actor,"OWNER","REVIEWER");if(!"IN_REVIEW".equals(p.status))throw new IllegalArgumentException("Cambio no permitido");}
    case "ARCHIVED" -> {auth.allowProject(pid,actor,"OWNER");if(!"APPROVED".equals(p.status))throw new IllegalArgumentException("Cambio no permitido");}
    default -> throw new IllegalArgumentException("Estado no reconocido");}
   p.status=next;return projects.save(p);
 }
}

