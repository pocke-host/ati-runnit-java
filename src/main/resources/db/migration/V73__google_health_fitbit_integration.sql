ALTER TABLE users
  ADD COLUMN fitbit_google_access_token TEXT NULL,
  ADD COLUMN fitbit_google_refresh_token TEXT NULL,
  ADD COLUMN fitbit_google_token_expires_at BIGINT NULL,
  ADD COLUMN fitbit_google_oauth_state VARCHAR(255) NULL,
  ADD COLUMN fitbit_google_health_user_id VARCHAR(255) NULL,
  ADD COLUMN fitbit_google_legacy_user_id VARCHAR(255) NULL,
  ADD COLUMN fitbit_google_last_sync TIMESTAMP NULL;

ALTER TABLE activities
  MODIFY COLUMN source ENUM('MANUAL', 'INTEGRATION', 'GARMIN', 'STRAVA', 'APPLE_WATCH', 'COROS', 'APPLE_HEALTH', 'WHOOP', 'OURA', 'FITBIT') DEFAULT 'MANUAL';
