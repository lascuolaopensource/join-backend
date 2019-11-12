# --- !Ups
ALTER TABLE activity_teach_event
  ADD COLUMN start_time TIMESTAMP WITHOUT TIME ZONE;


# --- !Downs
ALTER TABLE activity_teach_event
  DROP COLUMN start_time;
