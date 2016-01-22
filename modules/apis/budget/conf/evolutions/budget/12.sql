# --- !Ups

ALTER TABLE transaction_types
ADD COLUMN adjusted_transaction_type_id bigint REFERENCES transaction_types;

# --- !Downs
