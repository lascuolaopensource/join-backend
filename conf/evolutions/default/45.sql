# --- !Ups
ALTER TABLE "user"
  ADD COLUMN city VARCHAR(127),
  ADD COLUMN city_other BOOLEAN;

# --- !Downs
ALTER TABLE "user"
  DROP COLUMN city,
  DROP COLUMN city_other;
