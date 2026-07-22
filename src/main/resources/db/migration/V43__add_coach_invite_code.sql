ALTER TABLE users ADD COLUMN invite_code VARCHAR(20) DEFAULT NULL;
CREATE UNIQUE INDEX idx_users_invite_code ON users(invite_code);
