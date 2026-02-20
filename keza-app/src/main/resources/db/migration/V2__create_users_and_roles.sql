CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(255)    NOT NULL UNIQUE,
    phone               VARCHAR(20),
    password_hash       VARCHAR(255)    NOT NULL,
    first_name          VARCHAR(100)    NOT NULL,
    last_name           VARCHAR(100)    NOT NULL,
    user_type           VARCHAR(20)     NOT NULL DEFAULT 'INVESTOR',
    kyc_status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    email_verified      BOOLEAN         NOT NULL DEFAULT FALSE,
    phone_verified      BOOLEAN         NOT NULL DEFAULT FALSE,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    locked              BOOLEAN         NOT NULL DEFAULT FALSE,
    failed_login_attempts INT           NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    last_login_at       TIMESTAMPTZ,
    profile_image_url   VARCHAR(500),
    bio                 TEXT,
    date_of_birth       DATE,
    national_id         VARCHAR(50),
    kra_pin             VARCHAR(20),
    annual_income       DECIMAL(15,2),
    deleted             BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version             BIGINT          NOT NULL DEFAULT 0
);

CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)     NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version     BIGINT          NOT NULL DEFAULT 0
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Seed default roles
INSERT INTO roles (name, description) VALUES
    ('INVESTOR', 'Individual or institutional investor'),
    ('ISSUER', 'Company raising funds'),
    ('ADMIN', 'Platform administrator'),
    ('SUPER_ADMIN', 'Super administrator with full access');

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_phone ON users (phone);
CREATE INDEX idx_users_user_type ON users (user_type);
CREATE INDEX idx_users_kyc_status ON users (kyc_status);
CREATE INDEX idx_users_active ON users (active) WHERE active = TRUE;
CREATE INDEX idx_users_deleted ON users (deleted) WHERE deleted = FALSE;
