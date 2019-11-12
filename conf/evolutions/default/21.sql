# --- !Ups
ALTER TABLE "user"
  ADD COLUMN title VARCHAR(255);


# --- !Downs
ALTER TABLE "user"
  DROP COLUMN title;
