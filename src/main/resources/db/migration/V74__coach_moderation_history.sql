ALTER TABLE users
    ADD COLUMN coach_suspended TINYINT(1) NOT NULL DEFAULT 0;

CREATE TABLE coach_verification_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    verified TINYINT(1) NOT NULL,
    action VARCHAR(40) NOT NULL,
    note VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_coach_verification_history_coach (coach_id),
    INDEX idx_coach_verification_history_created (created_at)
);
