ALTER TABLE users ADD COLUMN coros_access_token TEXT;
ALTER TABLE users ADD COLUMN coros_refresh_token TEXT;
ALTER TABLE users ADD COLUMN coros_token_expires_at BIGINT;
ALTER TABLE users ADD COLUMN coros_oauth_state VARCHAR(100);
ALTER TABLE users ADD COLUMN coros_user_id VARCHAR(100);
ALTER TABLE users ADD COLUMN coros_last_sync TIMESTAMP;
