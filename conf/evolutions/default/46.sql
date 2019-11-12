# --- !Ups
CREATE TYPE BAZAAR_SCORE_DATA_T AS (
  creation  TIMESTAMP,
  agrees    BIGINT,
  favorites BIGINT,
  wishes    BIGINT,
  comments  BIGINT,
  views     BIGINT,
  deadline  INTEGER);

CREATE TYPE BAZAAR_IDEA_TYPE_T AS ENUM ('teach', 'event', 'research');

CREATE TYPE BAZAAR_IDEA_COUNTS_SCORE_T AS (
  score     DOUBLE PRECISION,
  agrees    BIGINT,
  favorites BIGINT,
  wishes    BIGINT,
  comments  BIGINT,
  views     BIGINT
);

CREATE TABLE bazaar_score_coefficients (
  alpha    DOUBLE PRECISION NOT NULL,
  beta     DOUBLE PRECISION NOT NULL,
  gamma    DOUBLE PRECISION NOT NULL,
  delta    DOUBLE PRECISION NOT NULL,
  t1       DOUBLE PRECISION NOT NULL,
  tH       DOUBLE PRECISION NOT NULL,
  deadline DOUBLE PRECISION NOT NULL
);

INSERT INTO bazaar_score_coefficients VALUES (1.05, 1.25, 1.3, 1.5, 3, 0.2, 90);

DROP TABLE bazaar_preference;
CREATE TABLE bazaar_preference (
  id                    BIGSERIAL PRIMARY KEY,
  bazaar_teach_learn_id BIGINT REFERENCES bazaar_teach_learn,
  bazaar_event_id       BIGINT REFERENCES bazaar_event,
  bazaar_research_id    BIGINT REFERENCES bazaar_research,
  user_id               BIGINT NOT NULL REFERENCES "user",
  wish_id               BIGINT REFERENCES bazaar_comment,
  agree                 TIMESTAMP,
  favorite              TIMESTAMP,
  viewed                TIMESTAMP,
  CONSTRAINT check_bazaar_idea
  CHECK ((bazaar_event_id IS NOT NULL) OR (bazaar_teach_learn_id IS NOT NULL) OR (bazaar_research_id IS NOT NULL))
);

CREATE OR REPLACE FUNCTION get_score_data_teach_learn(ideaId BIGINT, ts TIMESTAMP)
  RETURNS BAZAAR_SCORE_DATA_T AS
$BODY$
SELECT
  created_at,
  coalesce(agrees.count, 0)    AS agrees_c,
  coalesce(favorites.count, 0) AS favorites_c,
  coalesce(wishes.count, 0)    AS wishes_c,
  coalesce(commentsT.count, 0) AS comments_c,
  coalesce(views.count, 0)     AS views_c,
  -1                           AS deadline
FROM
  bazaar_teach_learn bl
  LEFT JOIN (SELECT
               bazaar_teach_learn_id AS eid,
               count(DISTINCT id)
             FROM bazaar_preference bp
             WHERE agree < ts
             GROUP BY eid) agrees
    ON agrees.eid = bl.id
  LEFT JOIN (SELECT
               bazaar_teach_learn_id AS eid,
               count(DISTINCT id)
             FROM bazaar_preference bp
             WHERE favorite < ts
             GROUP BY eid) favorites
    ON favorites.eid = bl.id
  LEFT JOIN (SELECT
               bp.bazaar_teach_learn_id AS eid,
               count(DISTINCT bp.id)
             FROM bazaar_preference bp
               JOIN bazaar_comment bc ON bp.wish_id = bc.id
             WHERE bc.created_at < ts
             GROUP BY eid) wishes
    ON bl.id = wishes.eid
  LEFT JOIN (SELECT
               bc.bazaar_teach_learn_id AS eid,
               count(DISTINCT bc.id)
             FROM bazaar_comment bc
               LEFT JOIN bazaar_preference bp ON bp.wish_id = bc.id
             WHERE wish_id IS NULL AND bc.created_at < ts
             GROUP BY eid) commentsT
    ON commentsT.eid = bl.id
  LEFT JOIN (SELECT
               bazaar_teach_learn_id AS eid,
               count(DISTINCT id)
             FROM bazaar_preference bp
             WHERE viewed < ts
             GROUP BY eid) views
    ON bl.id = views.eid
WHERE bl.id = ideaId;;
$BODY$
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION get_score_data_event(ideaId BIGINT, ts TIMESTAMP)
  RETURNS BAZAAR_SCORE_DATA_T AS
$BODY$
SELECT
  created_at,
  coalesce(agrees.count, 0)    AS agrees_c,
  coalesce(favorites.count, 0) AS favorites_c,
  coalesce(wishes.count, 0)    AS wishes_c,
  coalesce(commentsT.count, 0) AS comments_c,
  coalesce(views.count, 0)     AS views_c,
  -1                           AS deadline
FROM
  bazaar_event be
  LEFT JOIN (SELECT
               bazaar_event_id AS eid,
               count(DISTINCT id)
             FROM bazaar_preference bp
             WHERE agree < ts
             GROUP BY eid) agrees
    ON agrees.eid = be.id
  LEFT JOIN (SELECT
               bazaar_event_id AS eid,
               count(DISTINCT id)
             FROM bazaar_preference bp
             WHERE favorite < ts
             GROUP BY eid) favorites
    ON favorites.eid = be.id
  LEFT JOIN (SELECT
               bp.bazaar_event_id AS eid,
               count(DISTINCT bp.id)
             FROM bazaar_preference bp
              JOIN bazaar_comment bc ON bp.wish_id = bc.id
             WHERE bc.created_at < ts
             GROUP BY eid) wishes
    ON be.id = wishes.eid
  LEFT JOIN (SELECT
               bc.bazaar_event_id AS eid,
               count(DISTINCT bc.id)
             FROM bazaar_comment bc
               LEFT JOIN bazaar_preference bp ON bp.wish_id = bc.id
             WHERE wish_id IS NULL AND bc.created_at < ts
             GROUP BY eid) commentsT
    ON commentsT.eid = be.id
  LEFT JOIN (SELECT
               bazaar_event_id AS eid,
               count(DISTINCT id)
             FROM bazaar_preference bp
             WHERE viewed < ts
             GROUP BY eid) views
    ON be.id = views.eid
WHERE be.id = ideaId;;
$BODY$
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION get_score_data_research(ideaId BIGINT, ts TIMESTAMP)
  RETURNS BAZAAR_SCORE_DATA_T AS
$BODY$
SELECT
  created_at,
  coalesce(agrees.count, 0)    AS agrees_c,
  coalesce(favorites.count, 0) AS favorites_c,
  coalesce(wishes.count, 0)    AS wishes_c,
  coalesce(commentsT.count, 0) AS comments_c,
  coalesce(views.count, 0)     AS views_c,
  br.deadline                  AS deadline
FROM
  bazaar_research br
  LEFT JOIN (SELECT
               bazaar_research_id AS eid,
               count(DISTINCT id)
             FROM bazaar_preference bp
             WHERE agree < ts
             GROUP BY eid) agrees
    ON agrees.eid = br.id
  LEFT JOIN (SELECT
               bazaar_research_id AS eid,
               count(DISTINCT id)
             FROM bazaar_preference bp
             WHERE favorite < ts
             GROUP BY eid) favorites
    ON favorites.eid = br.id
  LEFT JOIN (SELECT
               bp.bazaar_research_id AS eid,
               count(DISTINCT bp.id)
             FROM bazaar_preference bp
               JOIN bazaar_comment bc ON bp.wish_id = bc.id
             WHERE bc.created_at < ts
             GROUP BY eid) wishes
    ON br.id = wishes.eid
  LEFT JOIN (SELECT
               bc.bazaar_research_id AS eid,
               count(DISTINCT bc.id)
             FROM bazaar_comment bc
               LEFT JOIN bazaar_preference bp ON bp.wish_id = bc.id
             WHERE wish_id IS NULL AND bc.created_at < ts
             GROUP BY eid) commentsT
    ON commentsT.eid = br.id
  LEFT JOIN (SELECT
               bazaar_research_id AS eid,
               count(DISTINCT id)
             FROM bazaar_preference bp
             WHERE viewed < ts
             GROUP BY eid) views
    ON br.id = views.eid
WHERE br.id = ideaId;;
$BODY$
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION scala_division(num DOUBLE PRECISION, den DOUBLE PRECISION)
  RETURNS DOUBLE PRECISION AS
$BODY$
DECLARE
  res DOUBLE PRECISION;;
BEGIN
  BEGIN
    res := num / den;;
    RETURN res;;
    EXCEPTION WHEN division_by_zero
    THEN
      IF num > 0
      THEN res := '+Infinity';;
      ELSIF num = 0
        THEN res := 'NaN';;
      ELSE res := '-Infinity';;
      END IF;;
      RETURN res;;
  END;;
END;;
$BODY$
LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_score(ideaId BIGINT, ideaType BAZAAR_IDEA_TYPE_T, ts TIMESTAMP)
  RETURNS BAZAAR_IDEA_COUNTS_SCORE_T AS
$BODY$
DECLARE
  r        RECORD;;
  coeff    RECORD;;
  deadline DOUBLE PRECISION;;
  theta    INTEGER;;
  fraction DOUBLE PRECISION;;
  ro       DOUBLE PRECISION;;
  epsilon  DOUBLE PRECISION;;
  fTheta   DOUBLE PRECISION;;
  res      BAZAAR_IDEA_COUNTS_SCORE_T;;
BEGIN
  SELECT *
  INTO coeff
  FROM bazaar_score_coefficients;;
  deadline := coeff.deadline;;

  CASE ideaType
    WHEN 'teach'
    THEN
      SELECT *
      INTO r
      FROM get_score_data_teach_learn(ideaId, ts);;
    WHEN 'event'
    THEN
      SELECT *
      INTO r
      FROM get_score_data_event(ideaId, ts);;
    WHEN 'research'
    THEN
      SELECT *
      INTO r
      FROM get_score_data_research(ideaId, ts);;
      deadline := r.deadline;;
  ELSE
    RETURN 0;;
  END CASE;;

  res.views := r.views;;
  res.wishes := r.wishes;;
  res.favorites := r.favorites;;
  res.agrees := r.agrees;;
  res.comments := r.comments;;

  theta := extract(DAY FROM (ts - r.creation));;

  fraction := scala_division(deadline + 1 + coeff.t1 * coeff.tH / (3 * (1 - coeff.tH)),
                             deadline * (1 + coeff.tH / (3 * (1 - coeff.tH))));;

  IF (theta <= coeff.t1)
  THEN
    fTheta := 1;;
  ELSIF (coeff.t1 < theta AND theta <= fraction * deadline)
    THEN
      ro := scala_division(coeff.tH - 1, fraction * deadline - coeff.t1);;
      fTheta := 1 - ro * coeff.t1 + ro * theta;;
  ELSIF (fraction * deadline < theta AND theta <= deadline)
    THEN
      epsilon := scala_division(coeff.tH, fraction * deadline - (deadline + 1));;
      fTheta := epsilon * theta - epsilon * (deadline + 1);;
  ELSE
    fTheta := 0;;
  END IF;;

  res.score := ((coeff.alpha * r.views) + (coeff.delta * r.agrees) + (coeff.gamma * r.wishes) + (coeff.beta * r.comments)) * fTheta;;

  RETURN res;;
END;;
$BODY$
LANGUAGE plpgsql;

CREATE OR REPLACE VIEW bazaar_teach_learn_s AS
  SELECT *
  FROM bazaar_teach_learn,
      get_score(id, 'teach', now()::TIMESTAMP);

CREATE OR REPLACE VIEW bazaar_event_s AS
  SELECT *
  FROM bazaar_event,
      get_score(id, 'event', now()::TIMESTAMP);

CREATE OR REPLACE VIEW bazaar_research_s AS
  SELECT *
  FROM bazaar_research,
      get_score(id, 'research', now()::TIMESTAMP);


# --- !Downs
DROP VIEW bazaar_teach_learn_s, bazaar_event_s, bazaar_research_s;

DROP FUNCTION get_score(ideaId BIGINT, ideaType BAZAAR_IDEA_TYPE_T, ts TIMESTAMP);
DROP FUNCTION scala_division(num DOUBLE PRECISION, den DOUBLE PRECISION );
DROP FUNCTION get_score_data_research(ideaId BIGINT, ts TIMESTAMP);
DROP FUNCTION get_score_data_event(ideaId BIGINT, ts TIMESTAMP);
DROP FUNCTION get_score_data_teach_learn(ideaId BIGINT, ts TIMESTAMP);

DROP TABLE bazaar_score_coefficients;

DROP TYPE BAZAAR_IDEA_COUNTS_SCORE_T;
DROP TYPE BAZAAR_IDEA_TYPE_T;
DROP TYPE BAZAAR_SCORE_DATA_T;
