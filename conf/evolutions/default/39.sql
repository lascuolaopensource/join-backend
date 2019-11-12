# --- !Ups
DROP TABLE bazaar_idea_space;

ALTER TABLE bazaar_event
  ADD COLUMN required_spaces VARCHAR(511);

# --- !Downs
ALTER TABLE bazaar_event
  DROP COLUMN required_spaces;
