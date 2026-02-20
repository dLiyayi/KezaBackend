CREATE TABLE investments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investor_id             UUID NOT NULL REFERENCES users(id),
    campaign_id             UUID NOT NULL REFERENCES campaigns(id),
    amount                  DECIMAL(15,2) NOT NULL,
    shares                  BIGINT NOT NULL,
    share_price             DECIMAL(15,4) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method          VARCHAR(20),
    cooling_off_expires_at  TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    cancellation_reason     TEXT,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_investor_campaign UNIQUE (investor_id, campaign_id)
);

CREATE TABLE transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investment_id       UUID REFERENCES investments(id),
    user_id             UUID NOT NULL REFERENCES users(id),
    type                VARCHAR(20) NOT NULL,
    amount              DECIMAL(15,2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'KES',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method      VARCHAR(20) NOT NULL,
    provider_reference  VARCHAR(255),
    provider_metadata   JSONB,
    description         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_investments_investor ON investments (investor_id);
CREATE INDEX idx_investments_campaign ON investments (campaign_id);
CREATE INDEX idx_investments_status ON investments (status);
CREATE INDEX idx_transactions_investment ON transactions (investment_id);
CREATE INDEX idx_transactions_user ON transactions (user_id);
CREATE INDEX idx_transactions_status ON transactions (status);
CREATE INDEX idx_transactions_provider_ref ON transactions (provider_reference);
