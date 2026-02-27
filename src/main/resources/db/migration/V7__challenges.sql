-- ============================================================
-- V7: Challenges, Leaderboards & Group Streaks
-- ============================================================

CREATE TABLE challenges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creator_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    sport_type VARCHAR(50),
    challenge_type VARCHAR(50) NOT NULL COMMENT 'DISTANCE, DURATION, COUNT, STREAK',
    goal_value DOUBLE NOT NULL COMMENT 'e.g. 50 for run-50km challenge',
    goal_unit VARCHAR(50) NOT NULL COMMENT 'KM, MILES, MINUTES, ACTIVITIES',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_group BOOLEAN NOT NULL DEFAULT false,
    max_participants INT,
    is_public BOOLEAN NOT NULL DEFAULT true,
    charity_name VARCHAR(200),
    charity_url VARCHAR(1000),
    cover_image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_creator (creator_id),
    INDEX idx_dates (start_date, end_date),
    INDEX idx_is_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE challenge_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    challenge_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    current_value DOUBLE NOT NULL DEFAULT 0,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    UNIQUE KEY uq_challenge_user (challenge_id, user_id),
    FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_challenge (challenge_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
