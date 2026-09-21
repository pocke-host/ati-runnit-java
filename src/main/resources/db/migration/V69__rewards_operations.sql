ALTER TABLE rewards_catalog
    ADD COLUMN fulfillment_type VARCHAR(30) NOT NULL DEFAULT 'INTERNAL',
    ADD COLUMN sizes_json TEXT NULL,
    ADD COLUMN tax_code VARCHAR(80) NULL,
    ADD COLUMN partner_name VARCHAR(160) NULL;

ALTER TABLE reward_redemptions
    ADD COLUMN apparel_size VARCHAR(30) NULL,
    ADD COLUMN subtotal_cents INT NULL,
    ADD COLUMN shipping_cents INT NULL,
    ADD COLUMN tax_cents INT NULL,
    ADD COLUMN total_cents INT NULL,
    ADD COLUMN shipping_method VARCHAR(80) NULL,
    ADD COLUMN external_checkout_session_id VARCHAR(255) NULL,
    ADD COLUMN cancellation_reason VARCHAR(255) NULL,
    ADD COLUMN shipped_at TIMESTAMP NULL,
    ADD COLUMN fulfilled_at TIMESTAMP NULL,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD INDEX idx_reward_redemptions_status_created (status, created_at),
    ADD UNIQUE KEY uq_reward_redemptions_checkout (external_checkout_session_id);

CREATE TABLE reward_coupon_codes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reward_id BIGINT NOT NULL,
    code VARCHAR(80) NOT NULL,
    discount_type VARCHAR(20) NOT NULL DEFAULT 'PERCENT',
    discount_value INT NOT NULL,
    max_redemptions INT NULL,
    redeemed_count INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_reward_coupon_code (code),
    INDEX idx_reward_coupon_reward (reward_id, active)
);

CREATE TABLE reward_admin_actions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    redemption_id BIGINT NOT NULL,
    admin_user_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    note VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reward_admin_actions_redemption (redemption_id, created_at)
);

CREATE TABLE reward_analytics_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    reward_id BIGINT NULL,
    redemption_id BIGINT NULL,
    event_type VARCHAR(40) NOT NULL,
    metadata_json TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reward_analytics_type_created (event_type, created_at),
    INDEX idx_reward_analytics_reward (reward_id, event_type)
);

CREATE TABLE reward_abuse_flags (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    flag_type VARCHAR(40) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    INDEX idx_reward_abuse_user_status (user_id, status)
);
