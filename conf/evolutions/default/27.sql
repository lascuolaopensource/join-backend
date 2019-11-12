# --- !Ups
ALTER TABLE "user"
  ADD dummy BOOLEAN NOT NULL DEFAULT FALSE;


# --- !Downs
ALTER TABLE "user"
  DROP COLUMN dummy;
