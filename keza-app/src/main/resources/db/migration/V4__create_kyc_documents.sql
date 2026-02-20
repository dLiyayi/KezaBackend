CREATE TABLE kyc_documents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    document_type       VARCHAR(50) NOT NULL,
    file_key            VARCHAR(500) NOT NULL,
    file_name           VARCHAR(255) NOT NULL,
    file_size           BIGINT NOT NULL,
    content_type        VARCHAR(100) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason    TEXT,
    extracted_data      JSONB,
    ai_confidence_score DECIMAL(5,4),
    reviewed_by         UUID REFERENCES users(id),
    reviewed_at         TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_kyc_documents_user ON kyc_documents (user_id);
CREATE INDEX idx_kyc_documents_status ON kyc_documents (status);
CREATE INDEX idx_kyc_documents_type ON kyc_documents (user_id, document_type);
