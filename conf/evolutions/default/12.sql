# --- !Ups
CREATE TABLE bazaar_idea (
  id                 BIGSERIAL     NOT NULL,
  creator_id         BIGINT        NOT NULL REFERENCES "user",
  title              VARCHAR(255)  NOT NULL,
  location           VARCHAR(255)  NOT NULL,
  activity_type      INTEGER       NOT NULL,
  days               INTEGER,
  recurring_days     INTEGER,
  recurring_every    INTEGER,
  recurring_entity   INTEGER,
  hours_per_meeting  INTEGER,
  level              INTEGER       NOT NULL,
  required_resources VARCHAR(511),
  max_participants   INTEGER       NOT NULL,
  program_details    VARCHAR(2047) NOT NULL,
  meeting_details    VARCHAR(2047) NOT NULL,
  output_details     VARCHAR(2047) NOT NULL,
  value_details      VARCHAR(2047) NOT NULL,
  motivation         VARCHAR(2047) NOT NULL,
  costs              VARCHAR(511),
  created_at         TIMESTAMP     NOT NULL,
  updated_at         TIMESTAMP     NOT NULL,
  deleted_at         TIMESTAMP,
  CONSTRAINT check_days_or_recurring CHECK (days IS NOT NULL OR
                                            (recurring_days IS NOT NULL AND
                                             recurring_every IS NOT NULL AND
                                             recurring_entity IS NOT NULL AND
                                             hours_per_meeting IS NOT NULL)),
  PRIMARY KEY (id)
);

CREATE TABLE bazaar_idea_framework (
  bazaar_idea_id BIGINT  NOT NULL REFERENCES bazaar_idea,
  framework_type INTEGER NOT NULL,
  PRIMARY KEY (bazaar_idea_id, framework_type)
);

CREATE TABLE bazaar_idea_audience (
  bazaar_idea_id BIGINT  NOT NULL REFERENCES bazaar_idea,
  audience_type  INTEGER NOT NULL,
  PRIMARY KEY (bazaar_idea_id, audience_type)
);

CREATE TABLE bazaar_idea_funding (
  bazaar_idea_id BIGINT  NOT NULL REFERENCES bazaar_idea,
  funding_type   INTEGER NOT NULL,
  PRIMARY KEY (bazaar_idea_id, funding_type)
);

CREATE TABLE bazaar_idea_meeting_duration (
  id             BIGSERIAL NOT NULL,
  bazaar_idea_id BIGINT    NOT NULL REFERENCES bazaar_idea,
  number_days    INTEGER   NOT NULL,
  number_hours   INTEGER   NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE bazaar_idea_date (
  bazaar_idea_id BIGINT NOT NULL REFERENCES bazaar_idea,
  date           DATE   NOT NULL,
  start_time     TIME   NOT NULL,
  end_time       TIME   NOT NULL,
  PRIMARY KEY (bazaar_idea_id, date)
);

CREATE TABLE bazaar_idea_teacher (
  id             BIGSERIAL NOT NULL,
  bazaar_idea_id BIGINT    NOT NULL REFERENCES bazaar_idea,
  user_id        BIGINT REFERENCES "user",
  name           VARCHAR(255),
  title          VARCHAR(255),
  tutor          BOOLEAN   NOT NULL,
  CONSTRAINT check_constraint CHECK (user_id IS NOT NULL OR (name IS NOT NULL AND title IS NOT NULL)),
  PRIMARY KEY (id)
);

CREATE TABLE topic (
  id    BIGSERIAL    NOT NULL,
  topic VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE bazaar_idea_topic (
  topic_id       BIGINT NOT NULL REFERENCES topic,
  bazaar_idea_id BIGINT NOT NULL REFERENCES bazaar_idea,
  PRIMARY KEY (topic_id, bazaar_idea_id)
);

CREATE TABLE bazaar_idea_space (
  id             BIGSERIAL NOT NULL,
  bazaar_idea_id BIGINT    NOT NULL REFERENCES bazaar_idea,
  sos_space      INTEGER,
  custom_space   VARCHAR(511),
  CONSTRAINT check_constraint CHECK (sos_space IS NOT NULL OR custom_space IS NOT NULL),
  PRIMARY KEY (id)
);


# --- !Downs
DROP TABLE bazaar_idea_framework;
DROP TABLE bazaar_idea_audience;
DROP TABLE bazaar_idea_funding;
DROP TABLE bazaar_idea_meeting_duration;
DROP TABLE bazaar_idea_date;
DROP TABLE bazaar_idea_teacher;
DROP TABLE bazaar_idea_topic;
DROP TABLE topic;
DROP TABLE bazaar_idea_space;
DROP TABLE bazaar_idea;
