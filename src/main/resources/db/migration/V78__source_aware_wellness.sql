-- Preserve one daily wellness snapshot per provider so Garmin and WHOOP can
-- be compared without one integration overwriting the other.
ALTER TABLE wellness_daily DROP INDEX idx_wellness_daily_user_date;
CREATE UNIQUE INDEX idx_wellness_daily_user_date_source
    ON wellness_daily(user_id, date, source);
