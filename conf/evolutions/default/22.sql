# --- !Ups
CREATE TABLE user_favorite (
  user_id       BIGINT REFERENCES "user",
  other_user_id BIGINT REFERENCES "user",
  PRIMARY KEY (user_id, other_user_id)
);


# --- !Downs
DROP TABLE user_favorite;
