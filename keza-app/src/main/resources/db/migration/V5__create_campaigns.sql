CREATE TABLE campaigns (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issuer_id           UUID NOT NULL REFERENCES users(id),
    title               VARCHAR(255) NOT NULL,
    slug                VARCHAR(255) UNIQUE,
    tagline             VARCHAR(500),
    description         TEXT,
    industry            VARCHAR(100),
    company_name        VARCHAR(255),
    company_registration_number VARCHAR(100),
    company_website     VARCHAR(500),
    company_address     TEXT,
    offering_type       VARCHAR(50) NOT NULL DEFAULT 'EQUITY',
    target_amount       DECIMAL(15,2) NOT NULL,
    minimum_amount      DECIMAL(15,2),
    maximum_amount      DECIMAL(15,2),
    raised_amount       DECIMAL(15,2) NOT NULL DEFAULT 0,
    share_price         DECIMAL(15,4),
    total_shares        BIGINT,
    sold_shares         BIGINT NOT NULL DEFAULT 0,
    min_investment      DECIMAL(15,2) NOT NULL DEFAULT 1000,
    max_investment      DECIMAL(15,2),
    investor_count      INT NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    wizard_step         INT NOT NULL DEFAULT 1,
    start_date          TIMESTAMPTZ,
    end_date            TIMESTAMPTZ,
    funded_at           TIMESTAMPTZ,
    pitch_video_url     VARCHAR(500),
    financial_projections JSONB,
    risk_factors        TEXT,
    use_of_funds        JSONB,
    team_members        JSONB,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE campaign_media (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id     UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    file_key        VARCHAR(500) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_size       BIGINT NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    media_type      VARCHAR(20) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE campaign_updates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id     UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    title           VARCHAR(255) NOT NULL,
    content         TEXT NOT NULL,
    author_id       UUID NOT NULL REFERENCES users(id),
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_campaigns_issuer ON campaigns (issuer_id);
CREATE INDEX idx_campaigns_status ON campaigns (status);
CREATE INDEX idx_campaigns_slug ON campaigns (slug);
CREATE INDEX idx_campaigns_industry ON campaigns (industry);
CREATE INDEX idx_campaigns_search ON campaigns USING GIN (to_tsvector('english', coalesce(title,'') || ' ' || coalesce(description,'') || ' ' || coalesce(company_name,'')));
CREATE INDEX idx_campaign_media_campaign ON campaign_media (campaign_id);
CREATE INDEX idx_campaign_updates_campaign ON campaign_updates (campaign_id);
