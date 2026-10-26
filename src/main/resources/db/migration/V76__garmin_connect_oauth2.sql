-- Garmin Connect Developer Program OAuth 2.0 PKCE and push identity state.
-- PlanetScale/Vitess: run each ALTER TABLE statement independently.
ALTER TABLE users ADD COLUMN garmin_refresh_token TEXT NULL;
ALTER TABLE users ADD COLUMN garmin_token_expires_at BIGINT NULL;
ALTER TABLE users ADD COLUMN garmin_oauth_state VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN garmin_oauth_code_verifier VARCHAR(128) NULL;
ALTER TABLE users ADD COLUMN garmin_user_id VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN garmin_permissions TEXT NULL;
