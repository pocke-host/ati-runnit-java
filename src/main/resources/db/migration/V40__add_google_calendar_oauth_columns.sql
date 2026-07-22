ALTER TABLE users ADD COLUMN google_calendar_access_token TEXT DEFAULT NULL;
ALTER TABLE users ADD COLUMN google_calendar_refresh_token TEXT DEFAULT NULL;
ALTER TABLE users ADD COLUMN google_calendar_token_expires_at BIGINT DEFAULT NULL;
ALTER TABLE users ADD COLUMN google_calendar_oauth_state VARCHAR(255) DEFAULT NULL;
