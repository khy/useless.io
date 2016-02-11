# --- !Ups

ALTER TABLE transaction_types ADD COLUMN context_id bigint REFERENCES contexts;

UPDATE transaction_types SET context_id = (
  SELECT contexts.id FROM contexts
  WHERE contexts.name = 'Default' AND contexts.created_by_account = transaction_types.created_by_account
  LIMIT 1
);

# --- !Downs
