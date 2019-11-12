# --- !Ups
CREATE TABLE activity_teach_event_favorite (
  activity_id BIGINT NOT NULL REFERENCES activity_teach_event,
  user_id     BIGINT NOT NULL REFERENCES "user",
  PRIMARY KEY (activity_id, user_id)
);


# --- !Downs
DROP TABLE IF EXISTS activity_teach_event_favorite;
