# --- !Ups
ALTER TABLE "user"
  ADD COLUMN email_confirmed BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE user_email_token (
  token      VARCHAR(255) PRIMARY KEY,
  email      VARCHAR(255) NOT NULL,
  user_id    BIGINT REFERENCES "user" NOT NULL,
  expiration TIMESTAMP WITHOUT TIME ZONE,
  token_type INT NOT NULL
);


# --- !Downs
ALTER TABLE "user"
  DROP COLUMN email_confirmed;

DROP TABLE user_email_token;
