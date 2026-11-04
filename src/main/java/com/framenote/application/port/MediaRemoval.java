package com.framenote.application.port;

import java.io.IOException;

public interface MediaRemoval {
    void delete(String storageKey) throws IOException;
}
