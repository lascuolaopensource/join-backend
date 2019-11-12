# --- !Ups
ALTER TABLE "user"
  DROP COLUMN s3_profile_pic;

# --- !Downs
ALTER TABLE "user"
  ADD COLUMN s3_profile_pic VARCHAR(255);
