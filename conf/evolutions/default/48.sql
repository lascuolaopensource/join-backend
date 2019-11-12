# --- !Ups
ALTER TABLE user_favorite
  ADD COLUMN created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now();


# --- !Downs
ALTER TABLE user_favorite
  DROP COLUMN created_at;
