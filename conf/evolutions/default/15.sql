# --- !Ups
DROP TABLE IF EXISTS bazaar_idea_framework;
DROP TABLE IF EXISTS bazaar_idea_audience;
DROP TABLE IF EXISTS bazaar_idea_funding;
DROP TABLE IF EXISTS bazaar_idea_meeting_duration;
DROP TABLE IF EXISTS bazaar_idea_date;
DROP TABLE IF EXISTS bazaar_idea_teacher;
DROP TABLE IF EXISTS bazaar_idea_topic;
DROP TABLE IF EXISTS bazaar_idea_space;
DROP TABLE IF EXISTS bazaar_idea;

CREATE TABLE bazaar_abstract_idea (
  id                 BIGSERIAL     NOT NULL,
  required_resources VARCHAR(511),
  max_participants   INTEGER       NOT NULL,
  program_details    VARCHAR(2047) NOT NULL,
  days               INTEGER,
  recurring_days     INTEGER,
  recurring_every    INTEGER,
  recurring_entity   INTEGER,
  hours_per_meeting  INTEGER,
  CONSTRAINT check_days_or_recurring CHECK (days IS NOT NULL OR
                                            (recurring_days IS NOT NULL AND
                                             recurring_every IS NOT NULL AND
                                             recurring_entity IS NOT NULL AND
                                             hours_per_meeting IS NOT NULL)),
  PRIMARY KEY (id)
);

CREATE TABLE bazaar_teach_learn (
  id                      BIGSERIAL     NOT NULL,
  creator_id              BIGINT        NOT NULL REFERENCES "user",
  title                   VARCHAR(255)  NOT NULL,
  value_details           VARCHAR(2047) NOT NULL,
  motivation              VARCHAR(2047) NOT NULL,
  location                VARCHAR(255)  NOT NULL,
  costs                   VARCHAR(511),

  type                    INTEGER       NOT NULL DEFAULT 0,

  activity_type           INTEGER,
  level                   INTEGER,
  meeting_details         VARCHAR(2047),
  output_details          VARCHAR(2047),
  bazaar_abstract_idea_id BIGINT REFERENCES bazaar_abstract_idea,
  created_at              TIMESTAMP     NOT NULL,
  updated_at              TIMESTAMP     NOT NULL,
  deleted_at              TIMESTAMP,

  CONSTRAINT check_teach CHECK (type != 1 OR
                                (activity_type IS NOT NULL AND level IS NOT NULL AND meeting_details IS NOT NULL
                                 AND output_details IS NOT NULL AND bazaar_abstract_idea_id IS NOT NULL)),

  PRIMARY KEY (id)
);

CREATE TABLE bazaar_event (
  id                      BIGSERIAL     NOT NULL,
  creator_id              BIGINT        NOT NULL REFERENCES "user",
  title                   VARCHAR(255)  NOT NULL,
  value_details           VARCHAR(2047) NOT NULL,
  motivation              VARCHAR(2047) NOT NULL,
  bazaar_abstract_idea_id BIGINT        NOT NULL REFERENCES bazaar_abstract_idea,
  activity_type           INTEGER       NOT NULL,
  is_organizer            BOOLEAN       NOT NULL,
  booking_required        BOOLEAN       NOT NULL,
  created_at              TIMESTAMP     NOT NULL,
  updated_at              TIMESTAMP     NOT NULL,
  deleted_at              TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE bazaar_idea_space (
  id             BIGSERIAL NOT NULL,
  bazaar_idea_id BIGINT    NOT NULL REFERENCES bazaar_event,
  sos_space      INTEGER,
  custom_space   VARCHAR(511),
  CONSTRAINT check_constraint CHECK (sos_space IS NOT NULL OR custom_space IS NOT NULL),
  PRIMARY KEY (id)
);

CREATE TABLE bazaar_idea_audience (
  bazaar_abstract_idea_id BIGINT  NOT NULL REFERENCES bazaar_abstract_idea,
  audience_type           INTEGER NOT NULL,
  PRIMARY KEY (bazaar_abstract_idea_id, audience_type)
);

CREATE TABLE bazaar_idea_funding (
  bazaar_abstract_idea_id BIGINT  NOT NULL REFERENCES bazaar_abstract_idea,
  funding_type            INTEGER NOT NULL,
  PRIMARY KEY (bazaar_abstract_idea_id, funding_type)
);

CREATE TABLE bazaar_idea_meeting_duration (
  id                      BIGSERIAL NOT NULL,
  bazaar_abstract_idea_id BIGINT    NOT NULL REFERENCES bazaar_abstract_idea,
  number_days             INTEGER   NOT NULL,
  number_hours            INTEGER   NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE bazaar_idea_date (
  id                      BIGSERIAL NOT NULL,
  bazaar_abstract_idea_id BIGINT NOT NULL REFERENCES bazaar_abstract_idea,
  date                    DATE   NOT NULL,
  start_time              TIME   NOT NULL,
  end_time                TIME   NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE bazaar_idea_guests (
  id                    BIGSERIAL NOT NULL,
  bazaar_event_id       BIGINT REFERENCES bazaar_event,
  bazaar_teach_learn_id BIGINT REFERENCES bazaar_teach_learn,
  user_id               BIGINT REFERENCES "user",
  first_name            VARCHAR(255),
  last_name             VARCHAR(255),
  title                 VARCHAR(255),
  tutor                 BOOLEAN   NOT NULL DEFAULT FALSE,
  CONSTRAINT check_bazaar_idea CHECK (bazaar_event_id IS NOT NULL OR bazaar_teach_learn_id IS NOT NULL),
  CONSTRAINT check_user_data CHECK (user_id IS NOT NULL OR
                                    (first_name IS NOT NULL AND last_name IS NOT NULL AND title IS NOT NULL)),
  PRIMARY KEY (id)
);

CREATE TABLE bazaar_idea_topic (
  id                    BIGSERIAL NOT NULL,
  topic_id              BIGINT    NOT NULL REFERENCES topic,
  bazaar_event_id       BIGINT REFERENCES bazaar_event,
  bazaar_teach_learn_id BIGINT REFERENCES bazaar_teach_learn,
  CONSTRAINT check_bazaar_idea CHECK (bazaar_event_id IS NOT NULL OR bazaar_teach_learn_id IS NOT NULL),
  PRIMARY KEY (id)
);


# --- !Downs
DROP TABLE IF EXISTS bazaar_idea_audience;
DROP TABLE IF EXISTS bazaar_idea_funding;
DROP TABLE IF EXISTS bazaar_idea_meeting_duration;
DROP TABLE IF EXISTS bazaar_idea_date;
DROP TABLE IF EXISTS bazaar_idea_guests;
DROP TABLE IF EXISTS bazaar_idea_topic;
DROP TABLE IF EXISTS bazaar_idea_space;
DROP TABLE IF EXISTS bazaar_event;
DROP TABLE IF EXISTS bazaar_teach_learn;
DROP TABLE IF EXISTS bazaar_abstract_idea;
