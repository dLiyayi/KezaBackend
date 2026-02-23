CREATE TABLE investment_events (
    id UUID PRIMARY KEY,
    investment_id UUID NOT NULL REFERENCES investments(id),
    user_id UUID NOT NULL REFERENCES users(id),
    event_type VARCHAR(50) NOT NULL,
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_investment_events_investment ON investment_events(investment_id);
CREATE INDEX idx_investment_events_user ON investment_events(user_id);
CREATE INDEX idx_investment_events_type ON investment_events(event_type);
CREATE INDEX idx_investment_events_created ON investment_events(created_at DESC);
