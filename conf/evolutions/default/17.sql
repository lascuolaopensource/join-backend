# --- !Ups
CREATE TABLE bazaar_comment (
  id                    BIGSERIAL     NOT NULL,
  user_id               BIGINT        NOT NULL REFERENCES "user",
  bazaar_teach_learn_id BIGINT REFERENCES bazaar_teach_learn,
  bazaar_event_id       BIGINT REFERENCES bazaar_event,
  comment               VARCHAR(2048) NOT NULL,
  created_at            TIMESTAMP NOT NULL,
  CONSTRAINT bazaar_idea CHECK (bazaar_teach_learn_id IS NOT NULL OR bazaar_event_id IS NOT NULL),
  PRIMARY KEY (id)
);

# --- !Downs
DROP TABLE IF EXISTS bazaar_comment;
