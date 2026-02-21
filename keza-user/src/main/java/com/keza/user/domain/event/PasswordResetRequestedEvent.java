package com.keza.user.domain.event;

import java.util.UUID;

public record PasswordResetRequestedEvent(
        UUID userId,
        String email,
        String firstName,
        String token
) {}
