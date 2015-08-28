# --- !Ups

CREATE TABLE accounts (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  type_key text NOT NULL,
  name text NOT NULL,
  initial_balance decimal,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid
);

CREATE TABLE projections (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  name text NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid
);

CREATE TABLE income_types (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  account_id bigint NOT NULL REFERENCES accounts,
  name text NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid
);

CREATE TABLE incomes (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  income_type_id bigint NOT NULL REFERENCES income_types,
  amount decimal NOT NULL,
  timestamp timestamp NOT NULL,
  projection_id bigint REFERENCES projections,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid
);

CREATE TABLE expense_types (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  account_id bigint NOT NULL REFERENCES accounts,
  name text NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid
);

CREATE TABLE expenses (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  expense_type_id bigint NOT NULL REFERENCES expense_types,
  amount decimal NOT NULL,
  timestamp timestamp NOT NULL,
  projection_id bigint REFERENCES projections,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid
);

# --- !Downs
