# --- !Ups
ALTER TABLE bazaar_preference
  ADD viewed BOOLEAN NOT NULL DEFAULT FALSE;

DROP TABLE bazaar_idea_date;

CREATE TABLE bazaar_idea_date (
  id                      BIGSERIAL              NOT NULL,
  bazaar_abstract_idea_id BIGINT                 NOT NULL REFERENCES bazaar_abstract_idea,
  date                    DATE                   NOT NULL,
  start_time              TIME WITHOUT TIME ZONE NOT NULL,
  end_time                TIME WITHOUT TIME ZONE NOT NULL,
  PRIMARY KEY (id)
);


# --- !Downs
ALTER TABLE bazaar_preference
  DROP COLUMN viewed;

DROP TABLE bazaar_idea_date;

CREATE TABLE bazaar_idea_date (
  id                      BIGSERIAL NOT NULL,
  bazaar_abstract_idea_id BIGINT    NOT NULL REFERENCES bazaar_abstract_idea,
  date                    DATE      NOT NULL,
  start_time              TIME      NOT NULL,
  end_time                TIME      NOT NULL,
  PRIMARY KEY (id)
);
