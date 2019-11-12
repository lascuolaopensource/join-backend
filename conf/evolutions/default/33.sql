# --- !Ups
CREATE TABLE fablab_reservation (
  id             BIGSERIAL,
  user_id        BIGINT REFERENCES "user"         NOT NULL,
  realization_of VARCHAR(512)                     NOT NULL,
  operator       BOOLEAN                          NOT NULL,
  created_at     TIMESTAMP                        NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE fablab_reservation_time (
  fablab_reservation_id BIGINT REFERENCES fablab_reservation NOT NULL,
  machine_id            BIGINT REFERENCES fablab_machine     NOT NULL,
  date                  DATE                                 NOT NULL,
  hour                  INT                                  NOT NULL,
  PRIMARY KEY (machine_id, date, hour)
);


# --- !Downs
DROP TABLE IF EXISTS fablab_reservation_time, fablab_reservation;
