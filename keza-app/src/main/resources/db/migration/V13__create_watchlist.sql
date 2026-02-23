CREATE TABLE watchlist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    campaign_id     UUID NOT NULL REFERENCES campaigns(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_watchlist_user_campaign UNIQUE (user_id, campaign_id)
);

CREATE INDEX idx_watchlist_user ON watchlist (user_id);
CREATE INDEX idx_watchlist_campaign ON watchlist (campaign_id);
