CREATE TABLE movements (
  guid uuid PRIMARY KEY,
  name text NOT NULL,
  variables jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamptz,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);
