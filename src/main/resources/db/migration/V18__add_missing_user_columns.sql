-- V18: Add user columns that were never formally migrated
-- Covers: user alias, profile fields, onboarding, Garmin OAuth
-- Safe to re-run: each ALTER TABLE only adds if column is new to the schema

ALTER TABLE users ADD COLUMN `user` VARCHAR(255) DEFAULT NULL;
ALTER TABLE users ADD COLUMN bio TEXT DEFAULT NULL;
ALTER TABLE users ADD COLUMN is_public TINYINT(1) DEFAULT 1;
ALTER TABLE users ADD COLUMN role VARCHAR(50) DEFAULT 'athlete';
ALTER TABLE users ADD COLUMN onboarding_complete TINYINT(1) DEFAULT 0;

ALTER TABLE users ADD COLUMN garmin_access_token TEXT DEFAULT NULL;
ALTER TABLE users ADD COLUMN garmin_access_token_secret TEXT DEFAULT NULL;
ALTER TABLE users ADD COLUMN garmin_request_token VARCHAR(200) DEFAULT NULL;
ALTER TABLE users ADD COLUMN garmin_request_token_secret TEXT DEFAULT NULL;
ALTER TABLE users ADD COLUMN garmin_last_sync DATETIME DEFAULT NULL;

-- Fix strava_token_expires_at: Java maps it as Long (epoch seconds), not DATETIME
ALTER TABLE users MODIFY COLUMN strava_token_expires_at BIGINT DEFAULT NULL;
