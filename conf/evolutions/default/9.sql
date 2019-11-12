# --- !Ups
ALTER TABLE "user"
    ADD COLUMN show_help_home BOOL DEFAULT TRUE;


# --- !Downs
ALTER TABLE "user" DROP COLUMN show_help_home;
