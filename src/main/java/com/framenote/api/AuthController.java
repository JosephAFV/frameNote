package com.framenote.api;
import com.framenote.application.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final AuthService service;public AuthController(AuthService s){service=s;}
 public record Registration(@Email @NotBlank String email,@NotBlank String name,@NotBlank String password){}
 public record Login(@NotBlank String email,@NotBlank String password){}
 @PostMapping("/register") public Map<String,Object> register(@Valid @RequestBody Registration r){return service.register(r.email(),r.password(),r.name());}
 @PostMapping("/login") public Map<String,String> login(@Valid @RequestBody Login r){return service.login(r.email(),r.password());}
}
