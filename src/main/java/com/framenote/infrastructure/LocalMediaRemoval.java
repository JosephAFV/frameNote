package com.framenote.infrastructure;

import com.framenote.application.port.MediaRemoval;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.*;

@Component
public class LocalMediaRemoval implements MediaRemoval {
    private final Path root = Paths.get(System.getenv().getOrDefault("MEDIA_DIR", "./media")).toAbsolutePath().normalize();

    public void delete(String storageKey) throws IOException {
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root) || target.equals(root)) throw new IOException("Ruta de medio inválida");
        Files.deleteIfExists(target);
    }
}
