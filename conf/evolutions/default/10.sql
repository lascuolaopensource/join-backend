# --- !Ups
ALTER TABLE "user" ADD COLUMN show_complete_profile BOOL DEFAULT TRUE;
ALTER TABLE "user" ADD COLUMN telephone VARCHAR(128);
ALTER TABLE "user" ADD COLUMN bio TEXT;


# --- !Downs
ALTER TABLE "user" DROP COLUMN show_complete_profile;
ALTER TABLE "user" DROP COLUMN telephone;
ALTER TABLE "user" DROP COLUMN bio;
