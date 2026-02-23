CREATE TABLE campaign_questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id     UUID NOT NULL REFERENCES campaigns(id),
    asker_id        UUID NOT NULL REFERENCES users(id),
    question        TEXT NOT NULL,
    answer          TEXT,
    answerer_id     UUID REFERENCES users(id),
    answered_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_campaign_questions_campaign ON campaign_questions (campaign_id);
CREATE INDEX idx_campaign_questions_asker ON campaign_questions (asker_id);
