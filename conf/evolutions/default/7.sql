# --- !Ups
ALTER TABLE "user"
  ADD COLUMN preferred_lang VARCHAR(32) DEFAULT 'it';


# --- !Downs
ALTER TABLE "user" DROP COLUMN preferred_lang;
