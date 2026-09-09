CREATE TABLE IF NOT EXISTS race_results (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  race_name VARCHAR(255) NOT NULL,
  race_date DATE NULL,
  distance VARCHAR(40) NULL,
  finish_time_seconds INT NULL,
  placement INT NULL,
  source VARCHAR(40) NOT NULL,
  external_result_id VARCHAR(160) NULL,
  result_url VARCHAR(500) NULL,
  verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_race_result_external (user_id, source, external_result_id),
  INDEX idx_race_results_user_date (user_id, race_date)
);
