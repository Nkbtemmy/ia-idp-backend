-- Seed Data for SSO Service
-- This file contains initial data for development and testing

-- Note: This file is executed automatically by Spring Boot if present
-- Passwords are BCrypt hashed versions of the plain text passwords shown in comments

-- Insert Admin User (password: Admin123!)
INSERT INTO user_account (id, email, password, name, email_verified, provider, created_at, updated_at) 
VALUES (
    gen_random_uuid(),
    'admin@sso.com',
    '$2a$10$8K1p/a0dRTAYHxVBkqRw4OEFaAiPLr5m.obRVziNxLgX8/L/l/l/.',
    'System Administrator',
    true,
    'LOCAL',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- Insert Regular User (password: User123!)
INSERT INTO user_account (id, email, password, name, email_verified, provider, created_at, updated_at) 
VALUES (
    gen_random_uuid(),
    'user@example.com',
    '$2a$10$8K1p/a0dRTAYHxVBkqRw4OEFaAiPLr5m.obRVziNxLgX8/L/l/l/.',
    'John Doe',
    true,
    'LOCAL',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- Insert Unverified User (password: Test123!)
INSERT INTO user_account (id, email, password, name, email_verified, provider, created_at, updated_at) 
VALUES (
    gen_random_uuid(),
    'unverified@example.com',
    '$2a$10$8K1p/a0dRTAYHxVBkqRw4OEFaAiPLr5m.obRVziNxLgX8/L/l/l/.',
    'Jane Smith',
    false,
    'LOCAL',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- Insert LinkedIn User (password: LinkedIn123!)
INSERT INTO user_account (id, email, password, name, email_verified, provider, provider_id, created_at, updated_at) 
VALUES (
    gen_random_uuid(),
    'linkedin@example.com',
    '$2a$10$8K1p/a0dRTAYHxVBkqRw4OEFaAiPLr5m.obRVziNxLgX8/L/l/l/.',
    'LinkedIn User',
    true,
    'LINKEDIN',
    'linkedin_12345',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- Insert Demo User (password: Demo123!)
INSERT INTO user_account (id, email, password, name, email_verified, provider, created_at, updated_at) 
VALUES (
    gen_random_uuid(),
    'demo@sso.com',
    '$2a$10$8K1p/a0dRTAYHxVBkqRw4OEFaAiPLr5m.obRVziNxLgX8/L/l/l/.',
    'Demo User',
    true,
    'LOCAL',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;
