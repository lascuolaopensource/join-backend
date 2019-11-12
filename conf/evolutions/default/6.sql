# --- !Ups
CREATE TABLE course (
  id          BIGSERIAL,
  name        VARCHAR(255)  NOT NULL,
  description VARCHAR(1023) NOT NULL,
  created_at  TIMESTAMP DEFAULT now(),
  PRIMARY KEY (id)
);


# --- !Downs
DROP TABLE course;
