# --- !Ups
CREATE TABLE membership_type (
  id         BIGSERIAL PRIMARY KEY,
  price      DOUBLE PRECISION NOT NULL,
  position   SMALLINT         NOT NULL,
  created_at TIMESTAMP        NOT NULL
);

CREATE TABLE membership_type_t (
  membership_type_id BIGINT        NOT NULL REFERENCES membership_type,
  language           VARCHAR(32)   NOT NULL,
  name               VARCHAR(127)  NOT NULL,
  offer              VARCHAR(1024) NOT NULL,
  bottom             VARCHAR(1024) NOT NULL,
  PRIMARY KEY (membership_type_id, language)
);

INSERT INTO membership_type VALUES
  (1, 50, 0, now()),
  (2, 75, 1, now()),
  (3, 100, 2, now());

INSERT INTO membership_type_t VALUES
  (1, 'it', 'PRO', $$<ul><li>hai uno sconto del 20% su tutti i corsi.</li></ul>$$,
   $$sconti sui corsi a pagamento;;$$),
  (2, 'it', 'FABLAB', $$<ul><li>sei iscritto all'associazione e assicurato;;</li>
                      <li>hai accesso a corsi macchina gratuiti;;</li>
                      <li>hai un bonus l'utilizzo delle macchine.</li></ul>$$,
   $$iscrizione all'associazione e copertura assicurativa;;$$),
  (3, 'it', 'COMBO', $$<ul><li>hai uno sconto del 20% su tutti i corsi;;</li>
                      <li>sei iscritto all'associazione e assicurato;;</li>
                      <li>hai accesso a corsi macchina gratuiti;;</li>
                      <li>hai un bonus per l'utilizzo delle macchine.</li><ul>$$,
   $$sconti sui corsi a pagamento + iscrizione all'associazione e copertura assicurativa$$),
  (1, 'en', 'PRO', $$<ul><li>a 20% discount on all activities.</li></ul>$$,
   $$discount on paid activities;;$$),
  (2, 'en', 'FABLAB', $$<ul><li>you're member of the association and covered by insurance;;</li>
                      <li>you get access to all machinery usage courses;;</li>
                      <li>you get bonus on machinery usage.</li></ul>$$,
   $$enrollment to association and insurance coverage;;$$),
  (3, 'en', 'COMBO', $$<ul><li>20% discount on all activities;;</li>
                      <li>you're member of the association and covered by insurance;;</li>
                      <li>you get access to all machinery usage courses;;</li>
                      <li>you get bonus on machinery usage.</li><ul>$$,
   $$discount on paid activities + enrollment to association and insurance coverage$$);

DROP TABLE IF EXISTS membership;
CREATE TABLE membership (
  id                 BIGSERIAL PRIMARY KEY,
  membership_type_id BIGINT    NOT NULL REFERENCES membership_type,
  user_id            BIGINT    NOT NULL REFERENCES "user",
  requested_at       TIMESTAMP NOT NULL,
  accepted_at        TIMESTAMP,
  starts_at          TIMESTAMP,
  ends_at            TIMESTAMP
);


# --- !Downs
DROP TABLE membership, membership_type_t, membership_type;
