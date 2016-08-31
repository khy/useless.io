DELETE FROM notes WHERE edition_guid != (
  SELECT guid FROM editions
  WHERE page_count = 406
  AND book_guid = (
    SELECT guid FROM books WHERE title = 'The Marriage Plot'
  )
);

ALTER TABLE notes DROP COLUMN edition_guid;
ALTER TABLE notes ADD COLUMN isbn text;
UPDATE notes SET isbn = '9781250014764';
ALTER TABLE notes ALTER COLUMN isbn SET NOT NULL;

DROP TABLE editions;
DROP TABLE books;
DROP TABLE authors;~
DROP TABLE IF EXISTS play_evolutions;

CREATE TABLE edition_cache (
  id bigserial PRIMARY KEY,
  isbn text NOT NULL,
  title text NOT NULL,
  subtitle text,
  authors text[],
  page_count int NOT NULL,
  small_image_url text,
  large_image_url text,
  publisher text,
  published_at date,
  provider_key text NOT NULL,
  provider_id text,
  created_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE UNIQUE INDEX edition_cache_isbn_unq_idx
ON edition_cache (isbn)
WHERE deleted_at IS NULL;
