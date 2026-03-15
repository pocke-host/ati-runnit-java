CREATE TABLE multisport_events (
  id           BIGINT NOT NULL AUTO_INCREMENT,
  user_id      BIGINT NOT NULL,
  name         VARCHAR(200) NOT NULL,
  event_type   VARCHAR(50)  NOT NULL DEFAULT 'TRIATHLON',
  event_date   DATE         DEFAULT NULL,
  notes        TEXT         DEFAULT NULL,
  is_public    TINYINT(1)   DEFAULT 1,
  created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE multisport_event_activities (
  id             BIGINT NOT NULL AUTO_INCREMENT,
  event_id       BIGINT NOT NULL,
  activity_id    BIGINT NOT NULL,
  sequence_order INT NOT NULL DEFAULT 0,
  segment_label  VARCHAR(100) DEFAULT NULL,
  PRIMARY KEY (id)
);
