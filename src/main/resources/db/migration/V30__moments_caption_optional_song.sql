-- Make song fields optional (previously NOT NULL, but not all moments have a song)
ALTER TABLE moments MODIFY COLUMN song_title VARCHAR(255) NULL;
ALTER TABLE moments MODIFY COLUMN song_artist VARCHAR(255) NULL;

-- Add caption / text content to moments
ALTER TABLE moments ADD COLUMN caption VARCHAR(500) NULL;
