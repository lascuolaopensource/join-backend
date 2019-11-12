# --- !Ups
CREATE TABLE bazaar_preference (
  id                    BIGSERIAL NOT NULL,
  bazaar_teach_learn_id BIGINT REFERENCES bazaar_teach_learn,
  bazaar_event_id       BIGINT REFERENCES bazaar_event,
  user_id               BIGINT NOT NULL REFERENCES "user",
  agree                 BOOLEAN NOT NULL DEFAULT FALSE,
  wish                  BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT check_bazaar_idea CHECK (bazaar_teach_learn_id IS NOT NULL OR bazaar_event_id IS NOT NULL),
  PRIMARY KEY (id)
);

# --- !Downs
DROP TABLE IF EXISTS bazaar_preference;
