ALTER TABLE activities MODIFY COLUMN sport_type ENUM('RUN', 'BIKE', 'SWIM', 'HIKE', 'WALK', 'STRENGTH', 'OTHER') NOT NULL;

-- No FK constraints — PlanetScale doesn't support them. activity_id references activities.id.
CREATE TABLE strength_exercises (
    id BIGINT NOT NULL AUTO_INCREMENT,
    activity_id BIGINT NOT NULL,
    exercise_name VARCHAR(120) NOT NULL,
    sequence_order INT NOT NULL DEFAULT 0,
    notes VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_strength_exercises_activity_id ON strength_exercises(activity_id);

CREATE INDEX idx_strength_exercises_exercise_name ON strength_exercises(exercise_name);

-- No FK constraints. strength_exercise_id references strength_exercises.id.
CREATE TABLE strength_sets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    strength_exercise_id BIGINT NOT NULL,
    set_number INT NOT NULL,
    reps INT NOT NULL,
    weight_kg DECIMAL(6,2) DEFAULT NULL,
    is_warmup TINYINT(1) NOT NULL DEFAULT 0,
    rpe DECIMAL(3,1) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_strength_sets_exercise_id ON strength_sets(strength_exercise_id);
