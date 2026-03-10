-- V14: Add Strava OAuth integration columns to users table
ALTER TABLE users
    ADD COLUMN strava_athlete_id        BIGINT       DEFAULT NULL,
    ADD COLUMN strava_access_token      VARCHAR(512) DEFAULT NULL,
    ADD COLUMN strava_refresh_token     VARCHAR(512) DEFAULT NULL,
    ADD COLUMN strava_token_expires_at  DATETIME     DEFAULT NULL,
    ADD COLUMN strava_oauth_state       VARCHAR(255) DEFAULT NULL,
    ADD COLUMN strava_last_sync         DATETIME     DEFAULT NULL;
