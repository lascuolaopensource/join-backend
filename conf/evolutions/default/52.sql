# --- !Ups
ALTER TABLE fablab_quotation
  ADD COLUMN undertaken BOOLEAN NOT NULL DEFAULT FALSE;

# --- !Downs
ALTER TABLE fablab_quotation
  DROP COLUMN undertaken;
