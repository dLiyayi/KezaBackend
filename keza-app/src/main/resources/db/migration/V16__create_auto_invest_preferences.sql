CREATE TABLE auto_invest_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    enabled BOOLEAN NOT NULL DEFAULT false,
    budget_amount DECIMAL(15, 2) NOT NULL,
    remaining_budget DECIMAL(15, 2) NOT NULL,
    max_per_campaign DECIMAL(15, 2),
    industries TEXT[],
    min_target_amount DECIMAL(15, 2),
    max_target_amount DECIMAL(15, 2),
    offering_types TEXT[],
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_auto_invest_user UNIQUE (user_id)
);

CREATE INDEX idx_auto_invest_user ON auto_invest_preferences(user_id);
CREATE INDEX idx_auto_invest_enabled ON auto_invest_preferences(enabled) WHERE enabled = true;
