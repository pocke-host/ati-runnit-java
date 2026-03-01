-- Make moment_id nullable so comments can belong to either a moment or an activity
ALTER TABLE comments MODIFY COLUMN moment_id BIGINT NULL;
ALTER TABLE comments ADD COLUMN activity_id BIGINT;
ALTER TABLE comments ADD CONSTRAINT fk_comments_activity
    FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE;

-- Activity reactions table
CREATE TABLE activity_reactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_activity (user_id, activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
