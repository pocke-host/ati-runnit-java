# Runnit DB Migrations — Claude Instructions

## Rules for PlanetScale/Vitess
1. **No `IF NOT EXISTS`** in ALTER TABLE — not supported
2. **No multi-column ALTER TABLE** if any column might already exist — use separate statements
3. **No foreign key constraints** — PlanetScale doesn't enforce them, skip FK lines
4. **No `DELIMITER`** — stored procedures not supported in console
5. Each statement must run independently (PlanetScale console = one statement at a time)

## Current Version: V18
Next migration: **V19**

## Schema Summary
| Table | Key columns |
|-------|-------------|
| users | id, email, display_name, user, bio, role, is_public, onboarding_complete, unit_system, location, sport, password_hash, auth_provider, strava_* (BIGINT for expires), garmin_* |
| activities | id, user_id, sport_type ENUM, duration_seconds, distance_meters, source ENUM, elevation_meters, pace_seconds, title, notes, route_data TEXT, external_id |
| moments | id, user_id, photo_url, song_title, song_artist, song_link, day_key |
| reactions | moment reactions — type ENUM('LIKE','FIRE','CLAP') |
| activity_reactions | activity reactions — type VARCHAR(50) |
| notifications | id, user_id, actor_id, type, message, is_read |
| plans | id, user_id, name, sport, level, is_active, start_date, target_race_date, current_weekly_meters, target_seconds |
| plan_workouts | id, plan_id, day, title, description, duration_minutes, distance_meters, completed, workout_type, week_number, target_pace_seconds |
| workout_events | id, user_id, planned_date, title, workout_type, distance_meters, duration_minutes, source, completed |
| clubs | id, name, sport, description, owner_id, member_count |
| challenges | id, name, sport, end_date, participant_count |
| personal_records | one row per user, best_5k/10k/half/marathon (seconds), distances (meters), elevation (meters) |

## Type Rules
- Timestamps mapped to Java `Instant` → use `DATETIME` or `TIMESTAMP` in DB
- Epoch seconds mapped to Java `Long` → use `BIGINT` in DB (e.g. strava_token_expires_at)
- Booleans → `TINYINT(1)` in MySQL
