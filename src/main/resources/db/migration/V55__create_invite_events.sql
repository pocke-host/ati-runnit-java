-- Tracks two moments in the friend-invite funnel: a user copying their own invite
-- link (event_type = 'COPIED'), and someone opening /join/{code} (event_type =
-- 'VISITED'). visitor_id is null when the visitor wasn't authenticated at the time
-- of the hit, which is the proxy for "a new person clicked this" vs. an existing
-- user re-opening a link they'd already used.
CREATE TABLE invite_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    inviter_id BIGINT NOT NULL,
    visitor_id BIGINT,
    event_type VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_invite_events_inviter ON invite_events (inviter_id, event_type);
