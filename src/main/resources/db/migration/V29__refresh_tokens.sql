-- Refresh tokens for long-lived sessions (opaque, DB-stored, rotated on use)
CREATE TABLE refresh_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token       VARCHAR(255) NOT NULL,
    expires_at  DATETIME     NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uq_refresh_token_token ON refresh_tokens (token);
CREATE INDEX idx_refresh_token_user_id   ON refresh_tokens (user_id);
