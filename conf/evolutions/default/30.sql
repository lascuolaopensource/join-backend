# --- !Ups
ALTER TABLE activity_teach_event
  ALTER COLUMN costs TYPE DOUBLE PRECISION;

ALTER TABLE payment_info
  ALTER COLUMN amount TYPE DOUBLE PRECISION;


# --- !Downs
ALTER TABLE activity_teach_event
  ALTER COLUMN costs TYPE FLOAT;

ALTER TABLE payment_info
  ALTER COLUMN amount TYPE FLOAT;
