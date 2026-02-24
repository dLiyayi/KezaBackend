-- Issuer Applications
CREATE TABLE issuer_applications (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID NOT NULL REFERENCES users(id),
    company_name                VARCHAR(255) NOT NULL,
    company_registration_number VARCHAR(100),
    company_website             VARCHAR(500),
    industry                    VARCHAR(100),
    business_stage              VARCHAR(30) NOT NULL,
    funding_goal                DECIMAL(15,2) NOT NULL,
    regulation_type             VARCHAR(20) NOT NULL,
    pitch_summary               TEXT,
    status                      VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    account_manager_id          UUID REFERENCES users(id),
    reviewer_id                 UUID REFERENCES users(id),
    review_notes                TEXT,
    reviewed_at                 TIMESTAMPTZ,
    eligible_at                 TIMESTAMPTZ,
    rejected_reason             TEXT,
    deleted                     BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at                  TIMESTAMPTZ,
    created_by                  VARCHAR(255),
    updated_by                  VARCHAR(255),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_issuer_applications_user UNIQUE (user_id)
);

CREATE INDEX idx_issuer_applications_user ON issuer_applications (user_id);
CREATE INDEX idx_issuer_applications_status ON issuer_applications (status);
CREATE INDEX idx_issuer_applications_manager ON issuer_applications (account_manager_id);

-- Issuer Onboarding Steps
CREATE TABLE issuer_onboarding_steps (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id  UUID NOT NULL REFERENCES issuer_applications(id) ON DELETE CASCADE,
    phase           INT NOT NULL,
    step_name       VARCHAR(100) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at    TIMESTAMPTZ,
    completed_by    UUID REFERENCES users(id),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_onboarding_steps_application ON issuer_onboarding_steps (application_id);

-- Campaign Interest Registrations (Testing the Waters)
CREATE TABLE campaign_interest_registrations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id     UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id),
    email           VARCHAR(255),
    intended_amount DECIMAL(15,2),
    registered_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_campaign_interest_user UNIQUE (campaign_id, user_id)
);

CREATE INDEX idx_campaign_interest_campaign ON campaign_interest_registrations (campaign_id);
CREATE INDEX idx_campaign_interest_user ON campaign_interest_registrations (user_id);

-- Fund Disbursements
CREATE TABLE fund_disbursements (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id         UUID NOT NULL REFERENCES campaigns(id),
    amount              DECIMAL(15,2) NOT NULL,
    disbursement_type   VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reference_number    VARCHAR(100),
    requested_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMPTZ,
    notes               TEXT,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_fund_disbursements_campaign ON fund_disbursements (campaign_id);
CREATE INDEX idx_fund_disbursements_status ON fund_disbursements (status);

-- Add testing_the_waters column to campaigns
ALTER TABLE campaigns ADD COLUMN testing_the_waters BOOLEAN NOT NULL DEFAULT FALSE;
