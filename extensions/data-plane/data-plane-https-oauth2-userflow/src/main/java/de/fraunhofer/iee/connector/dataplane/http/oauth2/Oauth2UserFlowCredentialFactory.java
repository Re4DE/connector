/*
 *  Copyright (c) 2025 Fraunhofer Institute for Energy Economics and Energy System Technology (IEE)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer IEE - initial API and implementation
 *
 */

package de.fraunhofer.iee.connector.dataplane.http.oauth2;

import de.fraunhofer.iee.iam.oauth2.spi.client.Oauth2UserFlowCredentialsRequest;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Optional;

import static de.fraunhofer.iee.iam.oauth2.spi.Oauth2UserFlowDataAddressSchema.*;

public class Oauth2UserFlowCredentialFactory {

    private static final String GRANT_PASSWORD = "password";

    private final Vault vault;

    public Oauth2UserFlowCredentialFactory(Vault vault) {
        this.vault = vault;
    }

    public Result<Oauth2UserFlowCredentialsRequest> create(DataAddress dataAddress) {
        var password = Optional.of(dataAddress)
                .map(address -> address.getStringProperty(PASSWORD_SECRET_NAME))
                .map(vault::resolveSecret)
                .orElse(null);

        if (password == null) {
            return Result.failure("Cannot resolve password from the vault: " + dataAddress.getStringProperty(PASSWORD_SECRET_NAME));
        }

        return Result.success(Oauth2UserFlowCredentialsRequest.Builder.newInstance()
                .url(dataAddress.getStringProperty(TOKEN_URL))
                .grantType(GRANT_PASSWORD)
                .username(dataAddress.getStringProperty(USERNAME))
                .password(password)
                .build());
    }
}
