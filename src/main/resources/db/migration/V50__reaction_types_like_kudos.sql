UPDATE reactions SET type = 'LIKE' WHERE type IN ('FIRE', 'CLAP');

ALTER TABLE reactions MODIFY COLUMN type ENUM('LIKE', 'KUDOS') NOT NULL;

UPDATE activity_reactions SET type = 'LIKE' WHERE type IN ('FIRE', 'CLAP');
