# --- !Ups

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
  user_uuid uuid NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE INDEX context_users_context_id_idx
  ON context_users (context_id);

INSERT INTO contexts (name, created_by_account, created_by_access_token)
  SELECT 'Default', created_by_account, created_by_access_token FROM accounts;

INSERT INTO context_users (context_id, user_uuid, created_by_account, created_by_access_token)
  SELECT id, created_by_account, created_by_account, created_by_access_token FROM contexts WHERE name = 'Default';

# --- !Downs
