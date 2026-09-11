ALTER TABLE users ADD COLUMN runsignup_access_token TEXT NULL;
ALTER TABLE users ADD COLUMN runsignup_refresh_token TEXT NULL;
ALTER TABLE users ADD COLUMN runsignup_token_expires_at BIGINT NULL;
ALTER TABLE users ADD COLUMN runsignup_oauth_state VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN runsignup_oauth_verifier VARCHAR(128) NULL;
