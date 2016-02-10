# --- !Ups

ALTER TABLE accounts ADD COLUMN context_id bigint REFERENCES contexts;

UPDATE accounts SET context_id = (
  SELECT contexts.id FROM contexts
  WHERE contexts.name = 'Default' AND contexts.created_by_account = accounts.created_by_account
  LIMIT 1
);

ALTER TABLE accounts ALTER COLUMN context_id SET NOT NULL;

# --- !Downs
