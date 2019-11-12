# --- !Ups
ALTER TABLE "user"
    ADD COLUMN agreement BOOL DEFAULT FALSE;
UPDATE "user" SET agreement = TRUE;


# --- !Downs
ALTER TABLE "user" DROP COLUMN agreement;
