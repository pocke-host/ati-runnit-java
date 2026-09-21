CREATE TABLE rewards_catalog (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description TEXT NULL,
    category VARCHAR(30) NOT NULL,
    reward_type VARCHAR(20) NOT NULL,
    points_cost INT NOT NULL DEFAULT 0,
    price_cents INT NULL,
    purchase_url VARCHAR(500) NULL,
    image_url VARCHAR(500) NULL,
    inventory INT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rewards_catalog_active (active)
);

CREATE TABLE rewards_ledger (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    points_delta INT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    event_key VARCHAR(180) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_rewards_ledger_user_event (user_id, event_key),
    INDEX idx_rewards_ledger_user_created (user_id, created_at)
);

CREATE TABLE reward_redemptions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reward_id BIGINT NOT NULL,
    points_cost INT NOT NULL DEFAULT 0,
    price_cents INT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    shipping_name VARCHAR(160) NULL,
    shipping_address TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reward_redemptions_user (user_id),
    INDEX idx_reward_redemptions_status (status)
);

INSERT INTO rewards_catalog (title, description, category, reward_type, points_cost, price_cents, inventory, active)
VALUES
    ('First-mile badge', 'A digital reward for your first kilometer.', 'DIGITAL', 'POINTS', 100, NULL, NULL, 1),
    ('Runnit partner discount', 'A rotating discount from a Runnit partner.', 'PARTNER', 'POINTS', 500, NULL, NULL, 1),
    ('Runnit running socks', 'Limited Runnit socks. Size and fulfillment are confirmed after redemption.', 'APPAREL', 'POINTS', 1500, NULL, 100, 1),
    ('Runnit training tee', 'Limited Runnit training tee. Size and fulfillment are confirmed after redemption.', 'APPAREL', 'POINTS', 3000, NULL, 100, 1),
    ('Partner recovery bundle', 'A partner product bundle. Follow the package label and consult a qualified professional for health questions.', 'SUPPLEMENTS', 'PAID', 0, 2499, 100, 1);
