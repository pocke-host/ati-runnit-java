-- V34: Workout library for saving reusable workout templates
CREATE TABLE workout_library (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    workout_type VARCHAR(50),
    sport       VARCHAR(50),
    distance_meters INT,
    duration_minutes INT,
    target_pace_seconds INT,
    notes       TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
