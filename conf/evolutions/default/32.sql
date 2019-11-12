# --- !Ups
CREATE TABLE fablab_machine (
  id                 BIGSERIAL,
  name               VARCHAR(512)     NOT NULL,
  work_area          VARCHAR(255),
  max_height         VARCHAR(255),
  cuts_metal         BOOLEAN,
  cuts_materials     VARCHAR(1024),
  engraves_metal     BOOLEAN,
  engraves_materials VARCHAR(1024),
  price_hour         DOUBLE PRECISION NOT NULL,
  operator           BOOLEAN          NOT NULL,
  PRIMARY KEY (id)
);


# --- !Downs
DROP TABLE IF EXISTS
fablab_machine;
