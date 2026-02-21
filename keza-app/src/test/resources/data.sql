-- Seed default roles for integration tests
INSERT INTO roles (id, name, description, created_at, updated_at, version) VALUES
    (gen_random_uuid(), 'INVESTOR', 'Individual or institutional investor', NOW(), NOW(), 0),
    (gen_random_uuid(), 'ISSUER', 'Company raising funds', NOW(), NOW(), 0),
    (gen_random_uuid(), 'ADMIN', 'Platform administrator', NOW(), NOW(), 0),
    (gen_random_uuid(), 'SUPER_ADMIN', 'Super administrator with full access', NOW(), NOW(), 0)
ON CONFLICT (name) DO NOTHING;
