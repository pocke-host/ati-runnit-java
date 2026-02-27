-- ============================================================
-- V8: Races & Events
-- ============================================================

CREATE TABLE races (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    race_type VARCHAR(50) NOT NULL COMMENT '5K, 10K, HALF_MARATHON, MARATHON, ULTRA, TRIATHLON, IRONMAN, OTHER',
    sport_type VARCHAR(50) NOT NULL DEFAULT 'RUN',
    race_date DATE NOT NULL,
    location_name VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100) DEFAULT 'US',
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    distance_meters INT,
    registration_url VARCHAR(1000),
    organizer_name VARCHAR(200),
    organizer_user_id BIGINT,
    cover_image_url VARCHAR(500),
    price_cents INT,
    is_featured BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organizer_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_race_date (race_date),
    INDEX idx_city_country (city, country),
    INDEX idx_is_featured (is_featured)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Users marking interest / registering for a race
CREATE TABLE race_interests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    race_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'INTERESTED' COMMENT 'INTERESTED, REGISTERED, COMPLETED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_race_user (race_id, user_id),
    FOREIGN KEY (race_id) REFERENCES races(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_race (race_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
