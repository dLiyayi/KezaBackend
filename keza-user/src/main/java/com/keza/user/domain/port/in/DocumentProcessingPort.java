package com.keza.user.domain.port.in;

import java.util.UUID;

public interface DocumentProcessingPort {
    void processDocument(UUID documentId);
}
