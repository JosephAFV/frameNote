package com.framenote.application;
import com.framenote.domain.*;
import com.framenote.application.port.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
@Service
public class AuthService {
 private final AccountStore accounts; private final WorkspaceStore workspaces; private final MembershipStore memberships;
 private final PasswordEncoder encoder; private final JwtEncoder jwt;
 public AuthService(AccountStore a,WorkspaceStore w,MembershipStore m,PasswordEncoder e,JwtEncoder j){accounts=a;workspaces=w;memberships=m;encoder=e;jwt=j;}
 @Transactional public Map<String,Object> register(String email,String password,String name){
   email=email.strip().toLowerCase(); if(accounts.findByEmail(email).isPresent())throw new IllegalArgumentException("Correo ya registrado");
   if(password==null||password.length()<10||name==null||name.isBlank())throw new IllegalArgumentException("Credenciales o nombre inválidos");
   Account a=new Account();a.email=email;a.name=name.strip();a.passwordHash=encoder.encode(password);accounts.save(a);
   Workspace w=new Workspace();w.name="Espacio de "+a.name;w.ownerId=a.id;w.plan="FREE";w.maxBytes=250L*1024*1024;w.maxProjects=1;workspaces.save(w);
   Membership m=new Membership();m.accountId=a.id;m.workspaceId=w.id;m.role="OWNER";memberships.save(m);
   return Map.of("token",token(a),"workspaceId",w.id,"accountId",a.id);
 }
 public Map<String,String> login(String email,String password){Account a=accounts.findByEmail(email.strip().toLowerCase()).orElseThrow(()->new IllegalArgumentException("Credenciales incorrectas"));
   if(!encoder.matches(password,a.passwordHash))throw new IllegalArgumentException("Credenciales incorrectas");return Map.of("token",token(a));}
 private String token(Account a){Instant now=Instant.now();JwtClaimsSet claims=JwtClaimsSet.builder().issuer("framenote").subject(a.id).issuedAt(now).expiresAt(now.plus(12,ChronoUnit.HOURS)).claim("email",a.email).build();return jwt.encode(JwtEncoderParameters.from(JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),claims)).getTokenValue();}
}

