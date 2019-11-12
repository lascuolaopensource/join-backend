# --- !Ups
CREATE TABLE "skill" (
  id     BIGSERIAL,
  "name" VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE "user_skill" (
  id         BIGSERIAL,
  user_id    BIGINT REFERENCES "user",
  skill_id   BIGINT REFERENCES "skill",
  created_at TIMESTAMP NOT NULL,
  PRIMARY KEY (id)
);


# --- !Downs
DROP TABLE "skill";
DROP TABLE "user_skill";
