# --- !Ups
CREATE TABLE password_info (
  id       BIGSERIAL,
  hasher   VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  salt     VARCHAR(255),
  user_id  BIGINT REFERENCES "user",
  PRIMARY KEY (id)
);


# --- !Downs
DROP TABLE password_info;
