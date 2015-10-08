# --- !Ups

CREATE TABLE planned_transactions (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  transaction_type_id bigint NOT NULL REFERENCES transaction_types,
  account_id bigint NOT NULL REFERENCES accounts,
  min_amount decimal,
  max_amount decimal,
  min_timestamp timestamp,
  max_timestamp timestamp,
  adjusted_planned_transaction_id bigint REFERENCES planned_transactions,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

INSERT INTO planned_transactions (guid, transaction_type_id, account_id, min_amount, max_amount, min_timestamp, max_timestamp, created_at, created_by_account, created_by_access_token, deleted_at, deleted_by_account, deleted_by_access_token)
SELECT guid, transaction_type_id, account_id, amount, amount, timestamp, timestamp, created_at, created_by_account, created_by_access_token, deleted_at, deleted_by_account, deleted_by_access_token
FROM transactions;

DELETE FROM transactions;

ALTER TABLE transactions
ADD COLUMN planned_transaction_id bigint REFERENCES planned_transactions;

ALTER TABLE transactions
ALTER COLUMN account_id SET NOT NULL;

# --- !Downs
