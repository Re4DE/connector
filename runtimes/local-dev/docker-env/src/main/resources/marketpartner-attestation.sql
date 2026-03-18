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
    market_role_mp_id   varchar                                         not null,
    market_role_name    varchar                                         not null,
    market_role_abbrv   varchar                                         not null,
    holder_id           varchar                                         not null,
    since               timestamp           default now()               not null,
    id                  varchar             default gen_random_uuid()   not null
        constraint markt_attestations_pk
            primary key
);

CREATE UNIQUE INDEX IF NOT EXISTS marketpartner_attestation_holder_id_uindex
    ON marketpartner_attestations (holder_id);