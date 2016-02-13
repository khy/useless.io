# --- !Ups

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE contexts (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  name text NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE INDEX contexts_guid_idx
  ON contexts (guid);

CREATE TABLE context_users (
  id bigserial PRIMARY KEY,
  context_id bigint NOT NULL REFERENCES contexts,
  user_guid uuid NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE INDEX context_users_context_id_idx
  ON context_users (context_id);

CREATE INDEX context_users_user_guid_idx
  ON context_users (user_guid);

INSERT INTO contexts (guid, name, created_by_account, created_by_access_token)
  SELECT gen_random_uuid(), 'Default', created_by_account, created_by_access_token
  FROM accounts GROUP BY created_by_account, created_by_access_token;

INSERT INTO context_users (context_id, user_guid, created_by_account, created_by_access_token)
  SELECT id, created_by_account, created_by_account, created_by_access_token FROM contexts WHERE name = 'Default';

# --- !Downs
