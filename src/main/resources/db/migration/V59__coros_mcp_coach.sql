ALTER TABLE users ADD COLUMN coros_mcp_access_token TEXT NULL;
ALTER TABLE users ADD COLUMN coros_mcp_refresh_token TEXT NULL;
ALTER TABLE users ADD COLUMN coros_mcp_token_expires_at BIGINT NULL;
ALTER TABLE users ADD COLUMN coros_mcp_oauth_state VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN coros_mcp_client_id VARCHAR(200) NULL;
