# --- !Ups
ALTER TABLE fablab_machine
  ADD COLUMN created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now();


# --- !Downs
ALTER TABLE fablab_machine
  DROP COLUMN created_at;
