CREATE TABLE training_folders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  description TEXT NULL,
  color VARCHAR(20) NULL,
  target_date DATE NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_training_folders_user (user_id)
);

CREATE TABLE training_folder_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  folder_id BIGINT NOT NULL,
  item_type VARCHAR(20) NOT NULL,
  item_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_training_folder_item (folder_id, item_type, item_id),
  INDEX idx_training_folder_items_folder (folder_id)
);
