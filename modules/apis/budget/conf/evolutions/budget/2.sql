# --- !Ups

CREATE TABLE transaction_confirmations (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  transaction_id bigint NOT NULL REFERENCES transactions,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

# --- !Downs

DROP TABLE transaction_confirmations CASCADE;
