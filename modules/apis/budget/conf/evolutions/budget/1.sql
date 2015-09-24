# --- !Ups

CREATE TABLE accounts (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  account_type_key text NOT NULL,
  name text NOT NULL,
  initial_balance decimal NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE INDEX accounts_guid_idx ON accounts (guid);

CREATE TABLE transaction_types (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  name text NOT NULL,
  parent_id bigint REFERENCES transaction_types,
  ownership_key text NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE INDEX transaction_types_guid_idx ON transaction_types (guid);
CREATE INDEX transaction_types_name_idx ON transaction_types (name);
CREATE INDEX transaction_types_parent_id_idx ON transaction_types (parent_id);

INSERT INTO transaction_types (guid, name, ownership_key, created_by_account, created_by_access_token)
VALUES ('a87efcf1-64c2-4949-87d0-e1f4849f743d', 'Income', 'system', '2a436fb0-7336-4f19-bde7-61570c05640c', '71a6828a-d20f-4fa6-8b2b-05a254487bda');

INSERT INTO transaction_types (guid, name, ownership_key, created_by_account, created_by_access_token)
VALUES ('16c1566a-10c0-4f99-a1f8-9aa2c230ba5d', 'Expense', 'system', '2a436fb0-7336-4f19-bde7-61570c05640c', '71a6828a-d20f-4fa6-8b2b-05a254487bda');

CREATE TABLE transactions (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  transaction_type_id bigint NOT NULL REFERENCES transaction_types,
  account_id bigint REFERENCES accounts,
  amount decimal NOT NULL,
  timestamp timestamp NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE INDEX transactions_guid_idx ON transactions (guid);
CREATE INDEX transactions_transaction_type_id_idx ON transactions (transaction_type_id);
CREATE INDEX transactions_account_id_idx ON transactions (account_id);

# --- !Downs

DROP TABLE accounts CASCADE;
DROP TABLE transaction_types CASCADE;
DROP TABLE transactions CASCADE;
