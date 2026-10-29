-- Keep provider readings auditable while allowing one canonical workout to be
-- shown in feeds, profiles, summaries, and exports.
-- PlanetScale/Vitess: run each ALTER TABLE statement independently.
ALTER TABLE activities ADD COLUMN canonical_activity_id BIGINT NULL;
ALTER TABLE activities ADD COLUMN source_match_confidence DECIMAL(5,4) NULL;
CREATE INDEX idx_activities_canonical_activity ON activities(canonical_activity_id);
