# --- !Ups
ALTER TABLE bazaar_preference
  DROP COLUMN wish;
ALTER TABLE bazaar_preference
  ADD wish_id BIGINT REFERENCES bazaar_comment;


# --- !Downs
ALTER TABLE bazaar_preference
  DROP COLUMN wish_id;
ALTER TABLE bazaar_preference
  ADD wish BOOLEAN NOT NULL DEFAULT FALSE;
