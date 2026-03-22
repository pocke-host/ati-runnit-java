-- Add Stripe customer ID so webhook events can be mapped to a user
-- without requiring an extra Stripe API call on every webhook.
ALTER TABLE users ADD COLUMN stripe_customer_id VARCHAR(100) DEFAULT NULL;

-- Store the Stripe subscription status (active, trialing, past_due, canceled, etc.)
-- so the /api/auth/me response can return subscriptionTier without hitting Stripe.
ALTER TABLE users ADD COLUMN subscription_status VARCHAR(50) DEFAULT NULL;
