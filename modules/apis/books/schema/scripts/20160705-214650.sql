CREATE TABLE notes (
  guid uuid PRIMARY KEY,
  edition_guid uuid NOT NULL REFERENCES editions(guid),
  page_number integer NOT NULL,
  content text NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE INDEX notes_content_fts_idx ON notes
USING gin(to_tsvector('english', content));
