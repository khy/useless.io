# --- !Ups

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE authors (
  guid uuid PRIMARY KEY,
  name text NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE UNIQUE INDEX authors_name_un_idx ON authors(name);

CREATE INDEX authors_name_fts_idx ON authors
USING gin(to_tsvector('simple', name));

# --- !Downs

DROP EXTENSION IF EXISTS pg_trgm;
DROP TABLE authors;
