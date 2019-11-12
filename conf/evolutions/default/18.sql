# --- !Ups
ALTER TABLE bazaar_preference
  ADD COLUMN favorite BOOLEAN NOT NULL DEFAULT FALSE;

# --- !Downs
ALTER TABLE bazaar_preference
  DROP COLUMN favorite;
