# --- !Ups
ALTER TABLE fablab_machine
  ADD COLUMN cuts_non_metal BOOLEAN,
  ADD COLUMN engraves_non_metal BOOLEAN;

# --- !Downs
ALTER TABLE fablab_machine
  DROP COLUMN cuts_non_metal,
  DROP COLUMN engraves_non_metal;
