# --- !Ups
CREATE TABLE activity_research (
  id                 BIGSERIAL    NOT NULL,
  cover_ext          VARCHAR(255) NOT NULL,
  gallery_id         BIGINT       NOT NULL REFERENCES image_gallery,
  organization_name  VARCHAR(255),
  deadline           DATE         NOT NULL,
  start_date         DATE         NOT NULL,
  duration           INT          NOT NULL,
  bazaar_research_id BIGINT REFERENCES bazaar_research,
  created_at         TIMESTAMP    NOT NULL,
  updated_at         TIMESTAMP    NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE activity_research_t (
  activity_id   BIGINT         NOT NULL REFERENCES activity_research,
  language      VARCHAR(7)     NOT NULL,
  title         VARCHAR(255)   NOT NULL,
  value_details VARCHAR(10240) NOT NULL,
  motivation    VARCHAR(10240) NOT NULL,
  PRIMARY KEY (activity_id, language)
);

CREATE TABLE activity_research_topic (
  activity_id BIGINT NOT NULL REFERENCES activity_research,
  topic_id    BIGINT NOT NULL REFERENCES topic,
  PRIMARY KEY (activity_id, topic_id)
);

CREATE TABLE activity_research_role (
  id                   BIGSERIAL NOT NULL,
  activity_research_id BIGINT    NOT NULL REFERENCES activity_research,
  people               INT       NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE activity_research_role_skill (
  activity_research_role_id BIGINT NOT NULL REFERENCES activity_research_role,
  skill_id                  BIGINT NOT NULL REFERENCES skill,
  PRIMARY KEY (activity_research_role_id, skill_id)
);

CREATE TABLE activity_research_role_application (
  activity_research_role_id BIGINT REFERENCES activity_research_role,
  user_id                   BIGINT REFERENCES "user",
  motivation                VARCHAR(1024),
  created_at                TIMESTAMP NOT NULL,
  PRIMARY KEY (activity_research_role_id, user_id)
);

CREATE TABLE activity_research_favorite (
  activity_id BIGINT REFERENCES activity_research,
  user_id     BIGINT REFERENCES "user",
  PRIMARY KEY (activity_id, user_id)
);


# --- !Downs
DROP TABLE IF EXISTS
activity_research_favorite,
activity_research_role_application,
activity_research_role_skill,
activity_research_role,
activity_research_topic,
activity_research_t,
activity_research;
