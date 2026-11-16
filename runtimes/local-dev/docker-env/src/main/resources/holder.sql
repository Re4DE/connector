-- Create the holder table. Normally, the issuer creates this database on its own, but we will use it to initialize with default participants
-- From holder-schema.sql - issuerservice-holder-store-sql - identity-hub
CREATE TABLE IF NOT EXISTS holders
(
    holder_id              VARCHAR PRIMARY KEY NOT NULL,              -- ID of the Holder
    participant_context_id VARCHAR             NOT NULL,              -- the DID with which this holder is identified
    did                    VARCHAR             NOT NULL,              -- the DID with which this holder is identified
    holder_name            VARCHAR,                                   -- the display name of the holder
    anonymous              BOOLEAN             NOT NULL DEFAULT FALSE,-- indicates whether the holder was created during a credential request
    created_date           BIGINT              NOT NULL,              -- POSIX timestamp of the creation of the PC
    last_modified_date     BIGINT,                                    -- POSIX timestamp of the last modified date
    properties             JSON DEFAULT '{}'                          -- JSON serialization of the properties
);
CREATE UNIQUE INDEX IF NOT EXISTS holders_holder_id_uindex ON holders USING btree (holder_id);

INSERT INTO holders (holder_id, participant_context_id, did, holder_name, created_date)
VALUES ('did:web:localhost%3A10085:tester', 'issuer', 'did:web:localhost%3A10085:tester', 'tester', 1779969883);
