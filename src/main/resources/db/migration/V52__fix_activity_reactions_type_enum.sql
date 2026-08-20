-- activity_reactions.type had drifted out of band (manually altered in the PlanetScale
-- console at some point to ENUM('CLAP','FIRE','HEART','STRONG','THUMBS_UP')) and was never
-- brought in line with V50's LIKE/KUDOS standardization, which only updated the `reactions`
-- table. Any insert of 'LIKE' or 'KUDOS' into activity_reactions failed with errno 1265
-- (data truncated) since neither value was a valid member of that enum.
ALTER TABLE activity_reactions MODIFY COLUMN type ENUM('CLAP','FIRE','HEART','STRONG','THUMBS_UP','LIKE','KUDOS') NOT NULL;

UPDATE activity_reactions SET type = 'LIKE' WHERE type IN ('CLAP','FIRE','HEART','STRONG','THUMBS_UP');

ALTER TABLE activity_reactions MODIFY COLUMN type ENUM('LIKE','KUDOS') NOT NULL;
