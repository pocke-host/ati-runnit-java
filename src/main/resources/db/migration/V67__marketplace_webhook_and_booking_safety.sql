CREATE TABLE stripe_webhook_events (
    event_id VARCHAR(255) NOT NULL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE coach_bookings
    ADD COLUMN cancellation_policy_hours INT NOT NULL DEFAULT 24,
    ADD COLUMN stripe_subscription_id VARCHAR(255) NULL;
