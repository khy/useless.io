# --- !Ups

ALTER TABLE transactions
ADD COLUMN adjusted_transaction_id bigint REFERENCES transactions;

# --- !Downs

ALTER TABLE transactions
DROP COLUMN adjusted_transaction_id;
