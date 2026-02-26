-- Seed test users (one per role) for development/testing
-- Password for all: Test@1234 (BCrypt strength 12)
-- These users should NOT be deployed to production

DO $$
DECLARE
    v_investor_id UUID := gen_random_uuid();
    v_issuer_id   UUID := gen_random_uuid();
    v_admin_id    UUID := gen_random_uuid();
    v_super_id    UUID := gen_random_uuid();
    v_hash        VARCHAR := '$2a$12$JBCy1t5/Sw.WVagqmaimd.itgzexVqFJrUAcx7x.P4FjCypA2BUTu';
BEGIN
    -- Create users
    INSERT INTO users (id, email, password_hash, first_name, last_name, user_type, email_verified, auth_provider)
    VALUES
        (v_investor_id, 'investor@keza.test', v_hash, 'Test', 'Investor', 'INVESTOR', TRUE, 'LOCAL'),
        (v_issuer_id,   'issuer@keza.test',   v_hash, 'Test', 'Issuer',   'ISSUER',   TRUE, 'LOCAL'),
        (v_admin_id,    'admin@keza.test',     v_hash, 'Test', 'Admin',    'ADMIN',    TRUE, 'LOCAL'),
        (v_super_id,    'superadmin@keza.test', v_hash, 'Test', 'SuperAdmin', 'ADMIN', TRUE, 'LOCAL')
    ON CONFLICT (email) DO NOTHING;

    -- Assign roles
    INSERT INTO user_roles (user_id, role_id)
    SELECT v_investor_id, id FROM roles WHERE name = 'INVESTOR'
    ON CONFLICT DO NOTHING;

    INSERT INTO user_roles (user_id, role_id)
    SELECT v_issuer_id, id FROM roles WHERE name = 'ISSUER'
    ON CONFLICT DO NOTHING;

    INSERT INTO user_roles (user_id, role_id)
    SELECT v_admin_id, id FROM roles WHERE name = 'ADMIN'
    ON CONFLICT DO NOTHING;

    INSERT INTO user_roles (user_id, role_id)
    SELECT v_super_id, id FROM roles WHERE name = 'SUPER_ADMIN'
    ON CONFLICT DO NOTHING;
END $$;
