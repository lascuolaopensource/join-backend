# --- !Ups
CREATE TABLE payment_info (
  id             BIGSERIAL NOT NULL PRIMARY KEY,
  payment_method SMALLINT  NOT NULL,
  transaction_id VARCHAR(255),
  cro            VARCHAR(255),
  verified       BOOLEAN,
  amount         FLOAT     NOT NULL,
  created_at     TIMESTAMP NOT NULL
);

CREATE TABLE activity_teach_event_sub (
  activity_id     BIGINT    NOT NULL REFERENCES activity_teach_event,
  user_id         BIGINT    NOT NULL REFERENCES "user",
  payment_info_id BIGINT REFERENCES payment_info,
  created_at      TIMESTAMP NOT NULL,
  PRIMARY KEY (activity_id, user_id)
);


# --- !Downs
DROP TABLE activity_teach_event_sub, payment_info;
