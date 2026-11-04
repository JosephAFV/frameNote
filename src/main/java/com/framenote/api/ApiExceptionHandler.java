package com.framenote.api;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.access.AccessDeniedException;
import java.util.Map;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
 @ExceptionHandler(java.util.NoSuchElementException.class) ResponseEntity<?> missing(Exception e){return ResponseEntity.status(404).body(Map.of("error","Recurso no encontrado"));}
 @ExceptionHandler(AccessDeniedException.class) ResponseEntity<?> forbidden(Exception e){return ResponseEntity.status(403).body(Map.of("error","Acceso denegado"));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(Exception e){return ResponseEntity.badRequest().body(Map.of("error","Datos inválidos"));}
}
