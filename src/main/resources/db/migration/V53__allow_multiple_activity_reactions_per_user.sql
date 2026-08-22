-- activity_reactions previously allowed at most one reaction row per (user, activity)
-- application-side (ActivityController upserted by user+activity, ignoring type) — liking
-- an activity silently replaced an existing kudos. Adding (user, activity, type) uniqueness
-- so a user can LIKE and KUDOS the same activity independently, while still preventing
-- duplicate rows of the same type.
--
-- NOTE: production never actually had the V8 `unique_user_activity (user_id, activity_id)`
-- key applied (SHOW INDEX confirms only PRIMARY exists) — another instance of the same kind
-- of console/migration-file drift as V52. A from-scratch environment that ran V8 as written
-- WOULD have that key and need it dropped first; production does not, so only the ADD ran there.
-- ALTER TABLE activity_reactions DROP INDEX unique_user_activity; -- only if it exists

ALTER TABLE activity_reactions ADD UNIQUE KEY unique_user_activity_type (user_id, activity_id, type);
