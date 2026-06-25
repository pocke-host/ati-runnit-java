CREATE TABLE waitlist_entries (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    email        VARCHAR(255) NOT NULL UNIQUE,
    name         VARCHAR(255),
    training_for VARCHAR(100),
    trains_with  VARCHAR(50),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
