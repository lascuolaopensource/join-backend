# --- !Ups
CREATE TABLE fablab_quotation (
  id             BIGSERIAL     NOT NULL PRIMARY KEY,
  user_id        BIGINT        NOT NULL REFERENCES "user",
  realization_of VARCHAR(1024) NOT NULL,
  created_at     TIMESTAMP     NOT NULL
);

CREATE TABLE fablab_quotation_machine (
  fablab_quotation_id BIGINT REFERENCES fablab_quotation,
  fablab_machine_id   BIGINT REFERENCES fablab_machine,
  PRIMARY KEY (fablab_quotation_id, fablab_machine_id)
);


# --- !Downs
DROP TABLE IF EXISTS fablab_quotation_machine, fablab_quotation;
