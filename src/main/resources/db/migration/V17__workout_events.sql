-- V17: Standalone calendar workout events (manual + AI-generated)
CREATE TABLE workout_events (
  id              BIGINT NOT NULL AUTO_INCREMENT,
  user_id         BIGINT NOT NULL,
  planned_date    DATE NOT NULL,
  title           VARCHAR(200) DEFAULT NULL,
  description     TEXT DEFAULT NULL,
  workout_type    VARCHAR(50)  DEFAULT NULL,
  distance_meters INT          DEFAULT NULL,
  duration_minutes INT         DEFAULT NULL,
  target_pace_seconds INT      DEFAULT NULL,
  notes           TEXT         DEFAULT NULL,
  source          VARCHAR(50)  DEFAULT 'MANUAL',
  completed       TINYINT(1)   DEFAULT 0,
  created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
