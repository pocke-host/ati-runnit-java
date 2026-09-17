ALTER TABLE users
    ADD COLUMN stripe_connect_account_id VARCHAR(100) NULL,
    ADD COLUMN coach_verified TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN coach_onboarding_complete TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN coach_specialties TEXT NULL,
    ADD COLUMN coach_experience TEXT NULL,
    ADD COLUMN coach_certifications TEXT NULL,
    ADD COLUMN coach_terms_accepted_at TIMESTAMP NULL;

CREATE TABLE coach_services (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NULL,
    service_type VARCHAR(30) NOT NULL,
    billing_type VARCHAR(20) NOT NULL,
    price_cents INT NOT NULL,
    duration_minutes INT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_coach_services_coach (coach_id),
    INDEX idx_coach_services_active (active)
);

CREATE TABLE coach_availability (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    coach_id BIGINT NOT NULL,
    weekday TINYINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    timezone VARCHAR(80) NOT NULL DEFAULT 'UTC',
    INDEX idx_coach_availability_coach (coach_id)
);

CREATE TABLE coach_bookings (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    service_id BIGINT NOT NULL,
    coach_id BIGINT NOT NULL,
    athlete_id BIGINT NOT NULL,
    scheduled_start TIMESTAMP NULL,
    scheduled_end TIMESTAMP NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    amount_cents INT NOT NULL,
    commission_cents INT NOT NULL,
    coach_amount_cents INT NOT NULL,
    stripe_checkout_session_id VARCHAR(200) NULL,
    stripe_payment_intent_id VARCHAR(200) NULL,
    cancellation_reason VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_coach_bookings_coach (coach_id),
    INDEX idx_coach_bookings_athlete (athlete_id),
    INDEX idx_coach_bookings_status (status)
);

CREATE TABLE coach_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    coach_id BIGINT NOT NULL,
    athlete_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    review_text VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_coach_reviews_coach (coach_id)
);
