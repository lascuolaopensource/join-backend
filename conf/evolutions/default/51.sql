# --- !Ups
SELECT setval('membership_type_id_seq', (SELECT MAX(id)
                                         FROM membership_type));

# --- !Downs
