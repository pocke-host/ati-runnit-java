CREATE TABLE wellness_daily (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'WHOOP',
    external_cycle_id VARCHAR(50),
    recovery_score INT,
    hrv_milli DOUBLE,
    resting_heart_rate INT,
    sleep_performance_pct INT,
    sleep_efficiency_pct DOUBLE,
    total_sleep_minutes INT,
    strain DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX idx_wellness_daily_user_date ON wellness_daily(user_id, date);
