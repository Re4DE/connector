--
--  Copyright (c) 2025 Fraunhofer Institute for Energy Economics and Energy System Technology (IEE)
--
--  This program and the accompanying materials are made available under the
--  terms of the Apache License, Version 2.0 which is available at
--  https://www.apache.org/licenses/LICENSE-2.0
--
--  SPDX-License-Identifier: Apache-2.0
--
--  Contributors:
--       Fraunhofer IEE - initial API and implementation
--

CREATE TABLE IF NOT EXISTS marketpartner_attestations
(
    company_name        varchar                                         not null,
    company_uid         varchar                                         not null,
    sector              varchar             default 'electricity'       not null,
    code_issuing_body   varchar             default 'BDEW'              not null,
    market_role         json                                            not null,
    holder_id           varchar                                         not null,
    id                  varchar             default gen_random_uuid()   not null
        constraint markt_attestations_pk
            primary key
);

CREATE UNIQUE INDEX IF NOT EXISTS marketpartner_attestation_holder_id_uindex
    ON marketpartner_attestations (holder_id);

INSERT INTO marketpartner_attestations (company_name, company_uid, market_role, holder_id)
VALUES ('tester inc.', '123456', '{"mpId":"123456","roleAbbreviation":"ESA","roleName":"Energieserviceanbieter"}', 'did:web:localhost%3A10085:tester');
