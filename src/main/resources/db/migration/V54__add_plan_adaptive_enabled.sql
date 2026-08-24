-- Lets an athlete pause AdaptivePlanService for a specific plan (e.g. a race/taper week
-- they want full manual control over) instead of the engine always being on.
ALTER TABLE plans ADD COLUMN adaptive_enabled TINYINT(1) NOT NULL DEFAULT 1;
