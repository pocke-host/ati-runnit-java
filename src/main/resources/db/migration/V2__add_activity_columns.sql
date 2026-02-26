-- Add missing columns to activities table
ALTER TABLE activities
ADD COLUMN calories INT,
ADD COLUMN elevation_gain INT,
ADD COLUMN average_heart_rate INT,
ADD COLUMN max_heart_rate INT,
ADD COLUMN average_pace DOUBLE,
ADD COLUMN route_polyline TEXT;
