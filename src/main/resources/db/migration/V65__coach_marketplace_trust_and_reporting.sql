ALTER TABLE users
    ADD COLUMN coach_privacy VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';

CREATE TABLE coach_reports (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    coach_id BIGINT NOT NULL,
    booking_id BIGINT NULL,
    reason VARCHAR(60) NOT NULL,
    details VARCHAR(2000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_coach_reports_coach (coach_id),
    INDEX idx_coach_reports_status (status)
);
