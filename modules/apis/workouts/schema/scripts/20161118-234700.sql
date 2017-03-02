CREATE TABLE workouts (
  guid uuid PRIMARY KEY,
  schema_version_major int NOT NULL,
  schema_version_minor int NOT NULL,
  parent_guids uuid[],
  json jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamptz,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);
