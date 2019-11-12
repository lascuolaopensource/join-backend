# --- !Ups
CREATE TABLE bazaar_research (
  id                  BIGSERIAL     NOT NULL,
  creator_id          BIGINT        NOT NULL REFERENCES "user",
  title               VARCHAR(255)  NOT NULL,
  value_details       VARCHAR(2047) NOT NULL,
  motivation          VARCHAR(2047) NOT NULL,
  organization_name   VARCHAR(255),
  required_resources  VARCHAR(511)  NOT NULL,
  deadline            INT           NOT NULL,
  duration            INT           NOT NULL,
  created_at          TIMESTAMP     NOT NULL,
  updated_at          TIMESTAMP     NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE bazaar_research_role (
  id                 BIGSERIAL NOT NULL,
  bazaar_research_id BIGINT    NOT NULL REFERENCES bazaar_research,
  people             INT       NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE bazaar_research_skill (
  bazaar_research_role_id BIGINT NOT NULL REFERENCES bazaar_research_role,
  skill_id                BIGINT NOT NULL REFERENCES skill,
  PRIMARY KEY (bazaar_research_role_id, skill_id)
);

ALTER TABLE bazaar_idea_topic
  ADD COLUMN bazaar_research_id BIGINT REFERENCES bazaar_research,
  DROP CONSTRAINT check_bazaar_idea,
  ADD CONSTRAINT check_bazaar_idea CHECK (bazaar_event_id IS NOT NULL OR
                                          bazaar_teach_learn_id IS NOT NULL OR
                                          bazaar_research_id IS NOT NULL);

ALTER TABLE bazaar_preference
  ADD COLUMN bazaar_research_id BIGINT REFERENCES bazaar_research,
  DROP CONSTRAINT check_bazaar_idea,
  ADD CONSTRAINT check_bazaar_idea CHECK (bazaar_event_id IS NOT NULL OR
                                          bazaar_teach_learn_id IS NOT NULL OR
                                          bazaar_research_id IS NOT NULL);

ALTER TABLE bazaar_comment
  ADD COLUMN bazaar_research_id BIGINT REFERENCES bazaar_research,
  DROP CONSTRAINT bazaar_idea,
  ADD CONSTRAINT check_bazaar_idea CHECK (bazaar_event_id IS NOT NULL OR
                                          bazaar_teach_learn_id IS NOT NULL OR
                                          bazaar_research_id IS NOT NULL);


# --- !Downs
ALTER TABLE bazaar_idea_topic
  DROP CONSTRAINT check_bazaar_idea,
  DROP COLUMN bazaar_research_id,
  ADD CONSTRAINT check_bazaar_idea CHECK (bazaar_event_id IS NOT NULL OR
                                          bazaar_teach_learn_id IS NOT NULL);

ALTER TABLE bazaar_preference
  DROP CONSTRAINT check_bazaar_idea,
  DROP COLUMN bazaar_research_id,
  ADD CONSTRAINT check_bazaar_idea CHECK (bazaar_event_id IS NOT NULL OR
                                          bazaar_teach_learn_id IS NOT NULL);

ALTER TABLE bazaar_comment
  DROP CONSTRAINT check_bazaar_idea,
  DROP COLUMN bazaar_research_id,
  ADD CONSTRAINT bazaar_idea CHECK (bazaar_event_id IS NOT NULL OR
                                    bazaar_teach_learn_id IS NOT NULL);

DROP TABLE bazaar_research_skill;
DROP TABLE bazaar_research_role;
DROP TABLE bazaar_research;
