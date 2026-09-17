-- V62 was never used by application logic. Remove its column if an environment
-- applied that historical migration; the dynamic check keeps new environments safe.
SET @drop_launch_pro = (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE users DROP COLUMN launch_pro_granted',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'launch_pro_granted'
);
PREPARE retire_launch_pro FROM @drop_launch_pro;
EXECUTE retire_launch_pro;
DEALLOCATE PREPARE retire_launch_pro;
