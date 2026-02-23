package com.keza.admin.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnnouncementResponse {

    private UUID id;
    private String title;
    private String content;
    private String type;
    private String priority;
    private boolean published;
    private Instant publishedAt;
    private Instant expiresAt;
    private UUID authorId;
    private String targetAudience;
    private Instant createdAt;
    private Instant updatedAt;
}
