package com.keza.user.domain.event;

import java.util.UUID;

public record KycStatusChangedEvent(
        UUID userId,
        String oldStatus,
        String newStatus,
        UUID documentId
) {}
