# --- !Ups
ALTER TABLE bazaar_idea_teacher
  RENAME name TO first_name;
ALTER TABLE bazaar_idea_teacher
  ADD COLUMN last_name VARCHAR(255);


# --- !Downs
ALTER TABLE bazaar_idea_teacher
  RENAME first_name TO name;
ALTER TABLE bazaar_idea_teacher
  DROP COLUMN last_name;
