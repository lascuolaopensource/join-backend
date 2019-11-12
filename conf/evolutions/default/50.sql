# --- !Ups
CREATE TABLE rules (
  text TEXT NOT NULL,
  unique_column BOOLEAN DEFAULT TRUE PRIMARY KEY,
  CHECK (unique_column IS TRUE)
);

# --- !Downs
DROP TABLE rules;
