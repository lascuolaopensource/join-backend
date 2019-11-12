# --- !Ups
ALTER TABLE bazaar_event
  ADD COLUMN disabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bazaar_teach_learn
  ADD COLUMN disabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bazaar_research
  ADD COLUMN disabled BOOLEAN NOT NULL DEFAULT FALSE;

# --- !Downs
ALTER TABLE bazaar_event
  DROP COLUMN disabled;
ALTER TABLE bazaar_teach_learn
  DROP COLUMN disabled;
ALTER TABLE bazaar_research
  DROP COLUMN disabled;
