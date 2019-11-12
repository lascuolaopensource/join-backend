# --- !Ups
ALTER TABLE "user"
  DROP COLUMN show_help_home,
  DROP COLUMN show_complete_profile;

# --- !Downs
ALTER TABLE "user"
  ADD COLUMN show_help_home BOOLEAN DEFAULT TRUE,
  ADD COLUMN show_complete_profile BOOLEAN DEFAULT TRUE;
