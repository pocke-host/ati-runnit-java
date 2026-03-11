-- V15: Add missing external_id column and expand source enum on activities

ALTER TABLE activities
    ADD COLUMN external_id VARCHAR(100) DEFAULT NULL;

ALTER TABLE activities
    MODIFY COLUMN source ENUM('MANUAL', 'INTEGRATION', 'GARMIN', 'STRAVA', 'APPLE_WATCH') DEFAULT 'MANUAL';
