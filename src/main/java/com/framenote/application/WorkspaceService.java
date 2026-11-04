package com.framenote.application;
import com.framenote.domain.*;
import com.framenote.application.port.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class WorkspaceService {
 public final WorkspaceStore workspaces;public final MembershipStore memberships;public final ProjectStore projects;public final ProjectMemberStore projectMembers;
 public WorkspaceService(WorkspaceStore w,MembershipStore m,ProjectStore p,ProjectMemberStore pm){workspaces=w;memberships=m;projects=p;projectMembers=pm;}
 public Workspace workspace(String id,String user){Workspace w=workspaces.findById(id).orElseThrow();memberships.findByWorkspaceIdAndAccountId(id,user).orElseThrow(()->new AccessDeniedException("Fuera del espacio"));return w;}
 public void owner(String wid,String user){Workspace w=workspace(wid,user);if(!w.ownerId.equals(user))throw new AccessDeniedException("Solo propietario");}
 public Project project(String pid,String user){Project p=projects.findById(pid).orElseThrow();workspace(p.workspaceId,user);if(projects.existsByIdAndStatus(pid,"DELETED"))throw new NoSuchElementException();if(!isProjectMember(pid,user))throw new AccessDeniedException("Sin acceso al proyecto");return p;}
 public boolean isProjectMember(String pid,String user){Project p=projects.findById(pid).orElseThrow();return p.workspaceId!=null && projectMembers.findByProjectIdAndAccountId(pid,user).isPresent();}
 public Project lockProject(String pid, String user) {
   Project project = project(pid,user);
   workspaces.lockById(project.workspaceId).orElseThrow();
   project.status = projects.findStatusById(pid).orElseThrow();
   if ("DELETED".equals(project.status)) throw new NoSuchElementException();
   return project;
 }
 public void allowProject(String pid,String user,String...roles){project(pid,user);ProjectMember m=projectMembers.findByProjectIdAndAccountId(pid,user).orElseThrow();if(!List.of(roles).contains(m.role))throw new AccessDeniedException("Rol insuficiente");}
}

