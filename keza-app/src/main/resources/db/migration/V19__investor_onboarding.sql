-- Add investor profile fields to users
ALTER TABLE users ADD COLUMN gender VARCHAR(20);
ALTER TABLE users ADD COLUMN country_of_residence VARCHAR(100);
ALTER TABLE users ADD COLUMN citizenship VARCHAR(100);
ALTER TABLE users ADD COLUMN net_worth DECIMAL(15,2);

-- Investment Accounts
CREATE TABLE investment_accounts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    account_type        VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    citizenship         VARCHAR(100),
    marital_status      VARCHAR(20),
    employment_status   VARCHAR(20),
    annual_income       DECIMAL(15,2),
    net_worth           DECIMAL(15,2),
    investment_experience VARCHAR(50),
    risk_tolerance      VARCHAR(20),
    opened_at           TIMESTAMPTZ,
    closed_at           TIMESTAMPTZ,
    processing_notes    TEXT,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_investment_accounts_user ON investment_accounts (user_id);
CREATE INDEX idx_investment_accounts_status ON investment_accounts (status);

-- Accreditation Verifications
CREATE TABLE accreditation_verifications (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id),
    accreditation_type      VARCHAR(20) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verification_method     VARCHAR(100),
    supporting_document_id  UUID REFERENCES kyc_documents(id),
    finra_crd_number        VARCHAR(50),
    verified_income         DECIMAL(15,2),
    verified_net_worth      DECIMAL(15,2),
    verified_at             TIMESTAMPTZ,
    expires_at              TIMESTAMPTZ,
    reviewer_id             UUID REFERENCES users(id),
    review_notes            TEXT,
    rejected_reason         TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_accreditation_user ON accreditation_verifications (user_id);
CREATE INDEX idx_accreditation_status ON accreditation_verifications (status);

-- Subscription Agreements (legal signature tracking for investments)
CREATE TABLE subscription_agreements (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investment_id       UUID NOT NULL REFERENCES investments(id),
    user_id             UUID NOT NULL REFERENCES users(id),
    campaign_id         UUID NOT NULL REFERENCES campaigns(id),
    agreement_version   VARCHAR(50) NOT NULL,
    signed_at           TIMESTAMPTZ,
    ip_address          VARCHAR(45),
    user_agent          VARCHAR(500),
    risk_acknowledged   BOOLEAN NOT NULL DEFAULT FALSE,
    countersigned       BOOLEAN NOT NULL DEFAULT FALSE,
    countersigned_at    TIMESTAMPTZ,
    document_url        VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_subscription_agreements_investment ON subscription_agreements (investment_id);
CREATE INDEX idx_subscription_agreements_user ON subscription_agreements (user_id);
