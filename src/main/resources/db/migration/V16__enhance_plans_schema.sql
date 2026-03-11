ALTER TABLE plans
  ADD COLUMN start_date DATE DEFAULT NULL,
  ADD COLUMN target_race_date DATE DEFAULT NULL,
  ADD COLUMN current_weekly_meters INT DEFAULT NULL,
  ADD COLUMN target_seconds INT DEFAULT NULL;

ALTER TABLE plan_workouts
  ADD COLUMN workout_type VARCHAR(50) DEFAULT NULL,
  ADD COLUMN week_number INT DEFAULT NULL,
  ADD COLUMN target_pace_seconds INT DEFAULT NULL;
