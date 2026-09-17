ALTER TABLE users
    ADD COLUMN monthly_rate DECIMAL(10,2) NULL,
    ADD COLUMN sports_coached TEXT NULL;

CREATE TABLE marketplace_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(40) NOT NULL,
    coach_id BIGINT NULL,
    athlete_id BIGINT NULL,
    service_id BIGINT NULL,
    booking_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_marketplace_events_type (event_type),
    INDEX idx_marketplace_events_coach (coach_id),
    INDEX idx_marketplace_events_created (created_at)
);
