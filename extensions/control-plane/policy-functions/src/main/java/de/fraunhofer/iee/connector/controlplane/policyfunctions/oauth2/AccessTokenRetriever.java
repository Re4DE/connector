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

package de.fraunhofer.iee.connector.controlplane.policyfunctions.oauth2;

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;

import static java.util.Optional.ofNullable;

public class AccessTokenRetriever {

    private final Oauth2Client client;
    private final Vault vault;
    private final String pmTokenUrl;
    private final String clientId;
    private final String clientSecretAlias;

    private static final String CLIENT_CREDENTIALS = "client_credentials";
    private static final String ESP_SCOPE = "esp";

    public AccessTokenRetriever(Oauth2Client client, Vault vault, String pmTokenUrl, String clientId, String clientSecretAlias) {
        this.client = client;
        this.vault = vault;
        this.pmTokenUrl = pmTokenUrl;
        this.clientId = clientId;
        this.clientSecretAlias = clientSecretAlias;
    }

    public Result<TokenRepresentation> obtainToken() {
        var clientSecret = ofNullable(this.vault.resolveSecret(this.clientSecretAlias))
                .orElseThrow(() -> new EdcException("Could not retrieve Permission Administrator Token Client Secret from vault, could not find secret with name: " + this.clientSecretAlias));

        var request = SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .url(this.pmTokenUrl)
                .grantType(CLIENT_CREDENTIALS)
                .clientId(this.clientId)
                .clientSecret(clientSecret)
                .scope(ESP_SCOPE)
                .build();

        return this.client.requestToken(request);
    }
}
