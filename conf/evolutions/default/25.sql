# --- !Ups
DROP TABLE IF EXISTS course;

CREATE TABLE image_gallery (
  id      BIGSERIAL    NOT NULL PRIMARY KEY,
  name    VARCHAR(255) NOT NULL
);

CREATE TABLE image_gallery_image (
  id         BIGSERIAL    NOT NULL PRIMARY KEY,
  gallery_id BIGINT       NOT NULL REFERENCES image_gallery,
  image_ext  VARCHAR(255) NOT NULL
);


CREATE TABLE activity_teach_event (
  id                    BIGSERIAL    NOT NULL PRIMARY KEY,
  is_teach              BOOLEAN      NOT NULL,
  cover_ext             VARCHAR(255) NOT NULL,
  gallery_id            BIGINT       NOT NULL REFERENCES image_gallery,
  level                 INT,
  activity_type         INT          NOT NULL,
  costs                 FLOAT,
  payments              BOOLEAN      NOT NULL,
  deadline              DATE,
  min_participants      INT,
  max_participants      INT,
  recurring_days        INT,
  recurring_every       INT,
  recurring_entity      INT,
  recurring_hours       INT,
  total_days            INT,
  total_hours           INT,
  teach_category        INT,
  bazaar_teach_learn_id BIGINT REFERENCES bazaar_teach_learn,
  bazaar_event_id       BIGINT REFERENCES bazaar_event,
  created_at            TIMESTAMP    NOT NULL,
  updated_at            TIMESTAMP    NOT NULL,
  CHECK ((recurring_days IS NOT NULL AND recurring_every IS NOT NULL
          AND recurring_entity IS NOT NULL AND recurring_hours IS NOT NULL) OR
         (total_days IS NOT NULL AND total_hours IS NOT NULL)),
  CHECK ((is_teach IS TRUE AND deadline IS NOT NULL AND teach_category IS NOT NULL
          AND bazaar_teach_learn_id IS NOT NULL AND bazaar_event_id IS NULL) OR
         (is_teach IS FALSE AND bazaar_event_id IS NOT NULL
          AND bazaar_teach_learn_id IS NULL AND teach_category IS NULL))
);

CREATE TABLE activity_teach_event_t (
  activity_id        BIGINT         NOT NULL REFERENCES activity_teach_event,
  language           VARCHAR(7)     NOT NULL,
  title              VARCHAR(255)   NOT NULL,
  description        VARCHAR(10240) NOT NULL,
  output_type        VARCHAR(255)   NOT NULL,
  output_description VARCHAR(10240),
  program            VARCHAR(10240) NOT NULL,
  PRIMARY KEY (activity_id, language)
);


CREATE TABLE activity_teach_event_topic (
  activity_id BIGINT NOT NULL REFERENCES activity_teach_event,
  topic_id    BIGINT NOT NULL REFERENCES topic,
  PRIMARY KEY (activity_id, topic_id)
);


CREATE TABLE activity_teach_event_audience (
  activity_id BIGINT NOT NULL REFERENCES activity_teach_event,
  audience    INT    NOT NULL,
  PRIMARY KEY (activity_id, audience)
);


CREATE TABLE activity_teach_event_skill (
  activity_id BIGINT  NOT NULL REFERENCES activity_teach_event,
  skill_id    BIGINT  NOT NULL REFERENCES skill,
  required    BOOLEAN NOT NULL,
  acquired    BOOLEAN NOT NULL,
  PRIMARY KEY (activity_id, skill_id),
  CHECK ((required IS TRUE AND acquired IS FALSE) OR
         (required IS FALSE AND acquired IS TRUE))
);


CREATE TABLE activity_teach_event_date (
  id          BIGSERIAL NOT NULL PRIMARY KEY,
  activity_id BIGINT    NOT NULL REFERENCES activity_teach_event,
  date        DATE      NOT NULL,
  start_time  TIME      NOT NULL,
  end_time    TIME      NOT NULL
);


CREATE TABLE activity_teach_event_guest (
  id          BIGSERIAL    NOT NULL PRIMARY KEY,
  activity_id BIGINT       NOT NULL REFERENCES activity_teach_event,
  user_id     BIGINT REFERENCES "user",
  first_name  VARCHAR(127) NOT NULL,
  last_name   VARCHAR(127) NOT NULL
);

CREATE TABLE activity_teach_event_guest_t (
  activity_teach_event_guest_id BIGINT        NOT NULL REFERENCES activity_teach_event_guest,
  language                      VARCHAR(7)    NOT NULL,
  title                         VARCHAR(255)  NOT NULL,
  bio                           VARCHAR(1048) NOT NULL,
  PRIMARY KEY (activity_teach_event_guest_id, language)
);


# --- !Downs
DROP TABLE IF EXISTS
activity_teach_event_guest_t,
activity_teach_event_guest,
activity_teach_event_date,
activity_teach_event_skill,
activity_teach_event_audience,
activity_teach_event_topic,
activity_teach_event_t,
activity_teach_event,
image_gallery_image,
image_gallery;
