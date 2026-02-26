-- Add social auth provider tracking to users table
ALTER TABLE users
    ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN auth_provider_id VARCHAR(255);
