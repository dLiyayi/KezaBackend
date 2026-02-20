CREATE TABLE due_diligence_checks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id     UUID NOT NULL REFERENCES campaigns(id),
    category        VARCHAR(50) NOT NULL,
    check_name      VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes           TEXT,
    checked_by      UUID REFERENCES users(id),
    checked_at      TIMESTAMPTZ,
    ai_result       VARCHAR(20),
    ai_confidence   DECIMAL(5,4),
    weight          DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE due_diligence_reports (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id         UUID NOT NULL REFERENCES campaigns(id) UNIQUE,
    total_checks        INT NOT NULL DEFAULT 0,
    passed_checks       INT NOT NULL DEFAULT 0,
    failed_checks       INT NOT NULL DEFAULT 0,
    na_checks           INT NOT NULL DEFAULT 0,
    overall_score       DECIMAL(5,2),
    risk_level          VARCHAR(20),
    recommendation      VARCHAR(20),
    summary             TEXT,
    generated_by        UUID REFERENCES users(id),
    generated_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_dd_checks_campaign ON due_diligence_checks (campaign_id);
CREATE INDEX idx_dd_checks_status ON due_diligence_checks (status);
CREATE INDEX idx_dd_reports_campaign ON due_diligence_reports (campaign_id);
