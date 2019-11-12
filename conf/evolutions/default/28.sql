# --- !Ups
ALTER TABLE activity_teach_event
  DROP CONSTRAINT activity_teach_event_check1,
  ADD CHECK ((is_teach IS TRUE AND deadline IS NOT NULL AND teach_category IS NOT NULL) OR
             (is_teach IS FALSE AND teach_category IS NULL));


# --- !Downs
ALTER TABLE activity_teach_event
  DROP CONSTRAINT activity_teach_event_check1,
  ADD CHECK ((is_teach IS TRUE AND deadline IS NOT NULL AND teach_category IS NOT NULL
              AND bazaar_teach_learn_id IS NOT NULL AND bazaar_event_id IS NULL) OR
             (is_teach IS FALSE AND bazaar_event_id IS NOT NULL
              AND bazaar_teach_learn_id IS NULL AND teach_category IS NULL));
