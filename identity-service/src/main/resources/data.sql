-- ============================================================
-- VEGGIEPAL IDENTITY SERVICE - SEED DATA
-- Users 1-3 are the authors of blog-service's seed posts, comments and votes
-- (blog-service/src/main/resources/data.sql). Nobody can sign in as them:
-- password_hash is not a BCrypt string, so matches() is always false, and the
-- .invalid TLD (RFC 2606) is never a real Google account.
-- A database whose ids 1-3 are already taken skips these rows.
-- ============================================================

INSERT INTO users (id, email, password_hash, full_name, role, status, email_verified, created_at, updated_at)
SELECT 1, 'author1@veggiepal.invalid', 'SEED_ACCOUNT_NO_LOGIN', 'VeggiePal Team', 'USER', 'ACTIVE', TRUE, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 1);

INSERT INTO users (id, email, password_hash, full_name, role, status, email_verified, created_at, updated_at)
SELECT 2, 'author2@veggiepal.invalid', 'SEED_ACCOUNT_NO_LOGIN', 'Bếp Chay Mỗi Ngày', 'USER', 'ACTIVE', TRUE, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 2);

INSERT INTO users (id, email, password_hash, full_name, role, status, email_verified, created_at, updated_at)
SELECT 3, 'author3@veggiepal.invalid', 'SEED_ACCOUNT_NO_LOGIN', 'Sống Xanh', 'USER', 'ACTIVE', TRUE, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 3);
