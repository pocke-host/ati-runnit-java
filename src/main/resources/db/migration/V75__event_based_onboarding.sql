ALTER TABLE users
    ADD COLUMN onboarding_goal VARCHAR(50) NULL,
    ADD COLUMN experience_level VARCHAR(50) NULL,
    ADD COLUMN training_days VARCHAR(20) NULL,
    ADD COLUMN target_event_id VARCHAR(100) NULL,
    ADD COLUMN target_event_provider VARCHAR(50) NULL,
    ADD COLUMN target_event_name VARCHAR(255) NULL,
    ADD COLUMN target_event_date DATE NULL,
    ADD COLUMN target_event_sport VARCHAR(50) NULL,
    ADD COLUMN target_event_url VARCHAR(500) NULL,
    ADD COLUMN target_event_city VARCHAR(100) NULL,
    ADD COLUMN target_event_state VARCHAR(100) NULL,
    ADD INDEX idx_users_target_event_date (target_event_date);
