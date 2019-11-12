# --- !Ups

CREATE TABLE "user" (
    id         BIGSERIAL,
    email      VARCHAR(255) NOT NULL,
    first_name VARCHAR(32) NOT NULL,
    last_name  VARCHAR(32) NOT NULL,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE "user";
