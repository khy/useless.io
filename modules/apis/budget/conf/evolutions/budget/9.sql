# --- !Ups

ALTER TABLE transactions ADD COLUMN name text;
ALTER TABLE planned_transactions ADD COLUMN name text;

# --- !Downs
