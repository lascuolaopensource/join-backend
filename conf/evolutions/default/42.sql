# --- !Ups
CREATE TABLE activity_research_team (
  id                   BIGSERIAL PRIMARY KEY,
  activity_research_id BIGINT REFERENCES activity_research NOT NULL,
  user_id              BIGINT REFERENCES "user",
  first_name           VARCHAR(127)                        NOT NULL,
  last_name            VARCHAR(127)                        NOT NULL,
  title                VARCHAR(255)                        NOT NULL
);


# --- !Downs
DROP TABLE activity_research_team;
