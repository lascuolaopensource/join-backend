# --- !Ups
ALTER TABLE activity_research
  ADD COLUMN project_link VARCHAR(255);


# --- !Downs
ALTER TABLE activity_research
  DROP COLUMN project_link;
