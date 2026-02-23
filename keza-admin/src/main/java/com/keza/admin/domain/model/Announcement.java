package com.keza.admin.domain.model;

import com.keza.common.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String type = "GENERAL";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String priority = "NORMAL";

    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "target_audience", length = 50)
    @Builder.Default
    private String targetAudience = "ALL";
}
