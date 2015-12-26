# --- !Ups

ALTER TABLE transactions ALTER COLUMN timestamp TYPE date;
ALTER TABLE transactions RENAME timestamp TO date;

ALTER TABLE planned_transactions ALTER COLUMN min_timestamp TYPE date;
ALTER TABLE planned_transactions RENAME min_timestamp TO min_date;

ALTER TABLE planned_transactions ALTER COLUMN max_timestamp TYPE date;
ALTER TABLE planned_transactions RENAME max_timestamp TO max_date;

# --- !Downs
