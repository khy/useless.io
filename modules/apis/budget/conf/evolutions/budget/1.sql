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

CREATE TABLE transaction_types (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  transaction_class_key text NOT NULL,
  account_id bigint NOT NULL REFERENCES accounts,
  name text NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE TABLE transactions (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  transaction_type_id bigint NOT NULL REFERENCES transaction_types,
  amount decimal NOT NULL,
  timestamp timestamp NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

# --- !Downs

DROP TABLE accounts CASCADE;
DROP TABLE transaction_types CASCADE;
DROP TABLE transactions CASCADE;
