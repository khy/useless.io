CREATE TABLE books (
  guid uuid PRIMARY KEY,
  title text NOT NULL,
  author_guid uuid NOT NULL REFERENCES authors(guid),
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE UNIQUE INDEX books_title_author_guid_un_idx ON books(title, author_guid);

CREATE INDEX books_title_fts_idx ON books
USING gin(to_tsvector('english', title));
