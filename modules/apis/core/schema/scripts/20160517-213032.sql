CREATE SCHEMA IF NOT EXISTS social;

CREATE TABLE social.likes (
  id bigserial NOT NULL PRIMARY KEY,
  guid uuid NOT NULL,
  resource_api text NOT NULL,
  resource_type text NOT NULL,
  resource_id text NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE INDEX social_likes_guid_idx ON social.likes (guid);
CREATE INDEX social_likes_resource_api_idx ON social.likes (resource_api);
CREATE INDEX social_likes_resource_type_idx ON social.likes (resource_type);
CREATE INDEX social_likes_resource_id_idx ON social.likes (resource_id);
