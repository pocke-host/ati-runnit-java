-- RUNNIT launch promise: first 100 accounts receive Pro for life.
-- V66 retired the historical, unused flag; this migration restores it for the
-- real entitlement service. Keep this idempotent because some environments
-- already have the column from the original launch rollout.
SET @add_launch_pro = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE users ADD COLUMN launch_pro_granted TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'launch_pro_granted'
);
PREPARE restore_launch_pro FROM @add_launch_pro;
EXECUTE restore_launch_pro;
DEALLOCATE PREPARE restore_launch_pro;
