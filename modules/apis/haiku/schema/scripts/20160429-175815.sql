CREATE TABLE haikus (
    id bigserial NOT NULL PRIMARY KEY,
    guid uuid NOT NULL,
    line_one text NOT NULL,
    line_two text NOT NULL,
    line_three text NOT NULL,
    in_response_to_id bigint REFERENCES haikus,
    attribution text,
    created_at timestamp NOT NULL DEFAULT now(),
    created_by_account uuid NOT NULL,
    created_by_access_token uuid NOT NULL,
    deleted_at timestamp,
    deleted_by_account uuid,
    deleted_by_access_token uuid
);

CREATE INDEX haikus_in_response_to_id_idx ON haikus (in_response_to_id);
CREATE INDEX haikus_created_by_account_idx ON haikus (created_by_account);
CREATE INDEX haikus_created_at_desc_idx ON haikus (created_at DESC);
