ALTER TABLE plan_workouts ADD COLUMN linked_activity_id BIGINT DEFAULT NULL;
ALTER TABLE plan_workouts ADD COLUMN target_heart_rate INT DEFAULT NULL;
ALTER TABLE plan_workouts ADD COLUMN target_rpe INT DEFAULT NULL;
ALTER TABLE plan_workouts ADD COLUMN adapted_at DATETIME DEFAULT NULL;
ALTER TABLE plan_workouts ADD COLUMN original_target_pace_seconds INT DEFAULT NULL;
ALTER TABLE plan_workouts ADD COLUMN original_duration_minutes INT DEFAULT NULL;
ALTER TABLE plan_workouts ADD COLUMN original_distance_meters INT DEFAULT NULL;
ALTER TABLE plan_workouts ADD COLUMN original_workout_type VARCHAR(50) DEFAULT NULL;

-- No FK constraints — PlanetScale doesn't support them
CREATE TABLE plan_adaptations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    plan_workout_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    triggered_by VARCHAR(20) NOT NULL,
    trigger_activity_id BIGINT DEFAULT NULL,
    reason TEXT NOT NULL,
    field_changed VARCHAR(50) NOT NULL,
    old_value VARCHAR(100) DEFAULT NULL,
    new_value VARCHAR(100) DEFAULT NULL,
    created_at DATETIME NOT NULL
);

CREATE INDEX idx_plan_adaptations_plan_id ON plan_adaptations (plan_id);
CREATE INDEX idx_plan_adaptations_user_id ON plan_adaptations (user_id);
