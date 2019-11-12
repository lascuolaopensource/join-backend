# --- !Ups
ALTER TABLE skill
  ADD COLUMN parent_id BIGINT REFERENCES skill,
  ADD COLUMN request BOOLEAN NOT NULL DEFAULT FALSE;


# --- !Downs
ALTER TABLE skill
  DROP COLUMN parent_id,
  DROP COLUMN request;
