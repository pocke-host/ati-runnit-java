-- Garmin Health API daily metrics for recovery-aware training guidance.
-- PlanetScale/Vitess: run each ALTER TABLE statement independently.
ALTER TABLE wellness_daily ADD COLUMN steps INT NULL;
ALTER TABLE wellness_daily ADD COLUMN intensity_minutes INT NULL;
ALTER TABLE wellness_daily ADD COLUMN active_kilocalories INT NULL;
ALTER TABLE wellness_daily ADD COLUMN average_stress DOUBLE NULL;
ALTER TABLE wellness_daily ADD COLUMN body_battery INT NULL;
ALTER TABLE wellness_daily ADD COLUMN respiration_breaths_per_minute DOUBLE NULL;
ALTER TABLE wellness_daily ADD COLUMN pulse_ox_percent DOUBLE NULL;
