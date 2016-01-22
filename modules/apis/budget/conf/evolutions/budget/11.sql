# --- !Ups

CREATE TABLE transaction_transaction_types (
  id bigserial PRIMARY KEY,
  transaction_id bigint NOT NULL REFERENCES transactions,
  transaction_type_id bigint NOT NULL REFERENCES transaction_types,
  adjusted_transaction_transaction_type_id bigint REFERENCES transaction_transaction_types,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE INDEX transaction_transaction_types_transaction_id_idx
  ON transaction_transaction_types (transaction_id);
CREATE INDEX transaction_transaction_types_transaction_type_id_idx
  ON transaction_transaction_types (transaction_type_id);
CREATE INDEX transaction_transaction_types_adjusted_transaction_transaction_type_id_idx
  ON transaction_transaction_types (adjusted_transaction_transaction_type_id);

INSERT INTO transaction_transaction_types (transaction_id, transaction_type_id, created_by_account, created_by_access_token)
SELECT id, transaction_type_id, created_by_account, created_by_access_token
FROM transactions;

ALTER TABLE transactions DROP COLUMN transaction_type_id;

# --- !Downs
