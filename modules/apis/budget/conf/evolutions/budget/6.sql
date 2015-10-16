# --- !Ups

CREATE TABLE transfers (
  id bigserial PRIMARY KEY,
  guid uuid NOT NULL,
  from_transaction_id bigint NOT NULL REFERENCES transactions,
  to_transaction_id bigint NOT NULL REFERENCES transactions,
  created_at timestamp NOT NULL DEFAULT now(),
  created_by_account uuid NOT NULL,
  created_by_access_token uuid NOT NULL,
  deleted_at timestamp,
  deleted_by_account uuid,
  deleted_by_access_token uuid
);

CREATE INDEX transfers_guid_idx ON transfers (guid);
CREATE INDEX transfers_from_transaction_id_idx ON transfers (from_transaction_id);
CREATE INDEX transfers_to_transaction_id_idx ON transfers (to_transaction_id);

# --- !Downs

DROP TABLE transfers CASCADE;
