# --- !Ups

CREATE TABLE transaction_type_subtypes (
  id bigserial PRIMARY KEY,
  parent_transaction_type_id bigint NOT NULL REFERENCES transaction_types,
  child_transaction_type_id bigint NOT NULL REFERENCES transaction_types,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE INDEX transaction_type_subtypes_parent_transaction_type_id_idx
  ON transaction_type_subtypes (parent_transaction_type_id);
CREATE INDEX transaction_type_subtypes_child_transaction_type_id_idx
  ON transaction_type_subtypes (child_transaction_type_id);

INSERT INTO transaction_type_subtypes (parent_transaction_type_id, child_transaction_type_id, created_by_account, created_by_access_token)
SELECT parent_id, id, created_by_account, created_by_access_token
FROM transaction_types WHERE parent_id IS NOT NULL;

ALTER TABLE transaction_types DROP COLUMN parent_id;

# --- !Downs
