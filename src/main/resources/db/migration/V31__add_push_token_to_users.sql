-- V31: Add APNs/FCM push token storage to users table
-- Run each statement separately in PlanetScale console

ALTER TABLE users ADD COLUMN push_token TEXT NULL;
ALTER TABLE users ADD COLUMN push_platform VARCHAR(20) NULL;
