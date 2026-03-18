ALTER TABLE clubs
  ADD COLUMN latitude  DOUBLE       DEFAULT NULL,
  ADD COLUMN longitude DOUBLE       DEFAULT NULL,
  ADD COLUMN city      VARCHAR(100) DEFAULT NULL;

CREATE TABLE emergency_contacts (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  user_id    BIGINT       NOT NULL,
  name       VARCHAR(100) NOT NULL,
  phone      VARCHAR(30)  DEFAULT NULL,
  email      VARCHAR(150) DEFAULT NULL,
  created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE sos_events (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  user_id    BIGINT       NOT NULL,
  lat        DOUBLE       DEFAULT NULL,
  lng        DOUBLE       DEFAULT NULL,
  share_url  VARCHAR(255) DEFAULT NULL,
  created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
