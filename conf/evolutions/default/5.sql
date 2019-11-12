# --- !Ups
ALTER TABLE "user"
  ADD COLUMN "role" INT NOT NULL DEFAULT 0,
  ADD COLUMN "created_at" TIMESTAMP NOT NULL DEFAULT NOW();


# --- !Downs
ALTER TABLE "user"
  DROP COLUMN "role",
  DROP COLUMN "created_at";
