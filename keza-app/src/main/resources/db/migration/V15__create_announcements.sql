CREATE TABLE announcements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(255) NOT NULL,
    content         TEXT NOT NULL,
    type            VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    priority        VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    author_id       UUID NOT NULL REFERENCES users(id),
    target_audience VARCHAR(50) DEFAULT 'ALL',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_announcements_published ON announcements (published, published_at);
CREATE INDEX idx_announcements_type ON announcements (type);
CREATE INDEX idx_announcements_target ON announcements (target_audience);
