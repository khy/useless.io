# --- !Ups

CREATE TABLE editions (
  guid uuid PRIMARY KEY,
  book_guid uuid NOT NULL REFERENCES books(guid),
  page_count integer NOT NULL,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

# --- !Downs

DROP TABLE editions;
