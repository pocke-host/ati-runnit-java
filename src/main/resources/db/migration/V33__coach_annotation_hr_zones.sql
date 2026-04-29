-- V33: Add coach annotation to activities and HR zones JSON to users
ALTER TABLE activities ADD COLUMN coach_annotation TEXT NULL;
ALTER TABLE users ADD COLUMN hr_zones_json TEXT NULL;
