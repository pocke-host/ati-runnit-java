-- ============================================================
-- V6: Training Plans (Adaptive, Runna-style)
-- ============================================================

-- User profile enhancements (role, bio, verified status for coaches)
ALTER TABLE users
    ADD COLUMN bio TEXT,
    ADD COLUMN role VARCHAR(50) NOT NULL DEFAULT 'ATHLETE',
    ADD COLUMN is_verified BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN is_mentor_available BOOLEAN NOT NULL DEFAULT false;

-- Training plans created by coaches / verified athletes
CREATE TABLE training_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creator_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    sport_type VARCHAR(50) NOT NULL DEFAULT 'RUN',
    difficulty_level VARCHAR(50) NOT NULL DEFAULT 'BEGINNER',
    duration_weeks INT NOT NULL DEFAULT 12,
    is_adaptive BOOLEAN NOT NULL DEFAULT true,
    is_verified BOOLEAN NOT NULL DEFAULT false,
    price_cents INT NOT NULL DEFAULT 0,
    tags VARCHAR(1000),
    cover_image_url VARCHAR(500),
    is_published BOOLEAN NOT NULL DEFAULT false,
    subscriber_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_creator (creator_id),
    INDEX idx_sport_type (sport_type),
    INDEX idx_is_published (is_published)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Individual workouts within a plan (organised by week + day)
CREATE TABLE training_plan_workouts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    week_number INT NOT NULL,
    day_of_week INT NOT NULL COMMENT '1=Monday … 7=Sunday',
    workout_type VARCHAR(50) NOT NULL COMMENT 'RECOVERY, BRICK, EASY_RUN, TEMPO, INTERVALS, LONG_RUN, REST, CROSS_TRAIN',
    title VARCHAR(200) NOT NULL,
    description TEXT,
    target_duration_minutes INT,
    target_distance_meters INT,
    target_heart_rate_zone INT COMMENT '1-5',
    intensity VARCHAR(50) NOT NULL DEFAULT 'MODERATE' COMMENT 'EASY, MODERATE, HARD, MAX',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (plan_id) REFERENCES training_plans(id) ON DELETE CASCADE,
    INDEX idx_plan_week (plan_id, week_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User subscriptions to training plans
CREATE TABLE training_plan_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    current_week INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, PAUSED, COMPLETED, CANCELLED',
    stripe_subscription_id VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_plan (user_id, plan_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES training_plans(id) ON DELETE CASCADE,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI-generated weekly adaptations per subscription
CREATE TABLE weekly_plan_adaptations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    week_number INT NOT NULL,
    adaptation_notes TEXT,
    volume_adjustment_percent INT NOT NULL DEFAULT 0,
    intensity_adjustment VARCHAR(50),
    ai_reasoning TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subscription_id) REFERENCES training_plan_subscriptions(id) ON DELETE CASCADE,
    INDEX idx_subscription_week (subscription_id, week_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
