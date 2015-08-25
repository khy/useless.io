# --- !Ups

CREATE TABLE accounts (
  id bigserial PRIMARY KEY,
  uuid uuid NOT NULL,
  type_key text NOT NULL,
  name text NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_uuid uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_uuid uuid
);

CREATE TABLE income_types (
  id bigserial PRIMARY KEY,
  uuid uuid NOT NULL,
  account_id bigint NOT NULL REFERENCES accounts,
  name text NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_uuid uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_uuid uuid
);

CREATE TABLE projected_incomes (
  id bigserial PRIMARY KEY,
  uuid uuid NOT NULL,
  income_type_id bigint NOT NULL REFERENCES income_types,
  amount decimal NOT NULL,
  timestamp timestamp NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_uuid uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_uuid uuid
);

CREATE TABLE actual_incomes (
  id bigserial PRIMARY KEY,
  uuid uuid NOT NULL,
  income_type_id bigint NOT NULL REFERENCES income_types,
  amount decimal NOT NULL,
  timestamp timestamp NOT NULL,
  projected_income_id bigint REFERENCES projected_incomes,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_uuid uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_uuid uuid
);

CREATE TABLE expense_types (
  id bigserial PRIMARY KEY,
  uuid uuid NOT NULL,
  account_id bigint NOT NULL REFERENCES accounts,
  name text NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_uuid uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_uuid uuid
);

CREATE TABLE projected_expenses (
  id bigserial PRIMARY KEY,
  uuid uuid NOT NULL,
  expense_type_id bigint NOT NULL REFERENCES expense_types,
  amount decimal NOT NULL,
  timestamp timestamp NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_uuid uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_uuid uuid
);

CREATE TABLE actual_expenses (
  id bigserial PRIMARY KEY,
  uuid uuid NOT NULL,
  expense_type_id bigint NOT NULL REFERENCES expense_types,
  amount decimal NOT NULL,
  timestamp timestamp NOT NULL,
  projected_expense_id bigint REFERENCES projected_expenses,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_uuid uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_uuid uuid
);

# --- !Downs
