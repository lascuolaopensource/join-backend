# --- !Ups
CREATE TABLE membership (
  id BIGSERIAL NOT NULL,
  type INTEGER NOT NULL,
  requested_at TIMESTAMP NOT NULL,
  accepted_at TIMESTAMP,
  starts_at TIMESTAMP,
  ends_at TIMESTAMP,
  deleted_at TIMESTAMP,
  user_id BIGINT NOT NULL REFERENCES "user",
  PRIMARY KEY (id)
);


# --- !Downs
DROP TABLE membership;
