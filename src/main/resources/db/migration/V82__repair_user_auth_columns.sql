-- Repair columns selected by the User entity but missing from older production
-- users tables. PlanetScale/Vitess: run each statement independently.
-- Do not re-run a statement for a column that already exists.

ALTER TABLE users ADD COLUMN coach_suspended TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE users ADD COLUMN onboarding_goal VARCHAR(50) NULL;
ALTER TABLE users ADD COLUMN experience_level VARCHAR(50) NULL;
ALTER TABLE users ADD COLUMN training_days VARCHAR(20) NULL;
ALTER TABLE users ADD COLUMN target_event_id VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN target_event_provider VARCHAR(50) NULL;
ALTER TABLE users ADD COLUMN target_event_name VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN target_event_date DATE NULL;
ALTER TABLE users ADD COLUMN target_event_sport VARCHAR(50) NULL;
ALTER TABLE users ADD COLUMN target_event_url VARCHAR(500) NULL;
ALTER TABLE users ADD COLUMN target_event_city VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN target_event_state VARCHAR(100) NULL;

ALTER TABLE users ADD COLUMN garmin_oauth_code_verifier VARCHAR(128) NULL;
ALTER TABLE users ADD COLUMN garmin_user_id VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN garmin_permissions TEXT NULL;

CREATE INDEX idx_users_target_event_date ON users(target_event_date);
