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

package de.fraunhofer.iee.connector.controlplane.fc.ssi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.List;

import static java.lang.String.format;
import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusNot2xxOr4xx;

public class CatalogNodeDirectory implements TargetNodeDirectory {

    private static final String CLIENT_CREDENTIALS = "client_credentials";

    private final EdcHttpClient client;
    private final Oauth2Client oauth2Client;
    private final Monitor monitor;
    private final String url;
    private final String tokenUrl;
    private final String tokenClientId;
    private final String tokenClientSecret;
    private final ObjectMapper mapper;

    public CatalogNodeDirectory(EdcHttpClient client, Oauth2Client oauth2Client, Monitor monitor, String url, String tokenUrl, String tokenClientId, String tokenClientSecret, ObjectMapper mapper) {
        this.client = client;
        this.oauth2Client = oauth2Client;
        this.monitor = monitor;
        this.url = url;
        this.tokenUrl = tokenUrl;
        this.tokenClientId = tokenClientId;
        this.tokenClientSecret = tokenClientSecret;
        this.mapper = mapper;
    }

    // Not implemented, the external connector registry gives a list of all available participants
    @Override
    public void insert(TargetNode targetNode) {}

    @Override
    public List<TargetNode> getAll() {
        this.monitor.debug("Getting all catalog nodes");

        try {
            return this.oauth2Client.requestToken(SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                            .url(tokenUrl)
                            .grantType(CLIENT_CREDENTIALS)
                            .clientId(tokenClientId)
                            .clientSecret(tokenClientSecret)
                            .build())
                    .map(token -> {
                        var request = new Request.Builder()
                                .url(url)
                                .header("Authorization", "Bearer " + token.getToken())
                                .method("GET", null)
                                .build();

                        return this.client.executeAsync(request, List.of(retryWhenStatusNot2xxOr4xx()))
                                .thenApply(this::handleResponse);
                    })
                    .orElseThrow(failure -> new EdcException(format("Unable to obtain credentials: %s", failure.getFailureDetail())))
                    .get();
        } catch (Exception e) {
            throw new EdcException(e);
        }
    }

    // Not implemented, the external connector registry gives a list of all available participants
    @Override
    public TargetNode remove(String id) {
        return null;
    }

    private List<TargetNode> handleResponse(Response response) {
        if (!response.isSuccessful()) {
            this.monitor.debug("Fetch target nodes failed, response not 200, is: " + response.code() + " " + response.message());
            response.close();
            return List.of();
        }

        if (response.body() == null) {
            this.monitor.debug("Response body is null");
            response.close();
            return List.of();
        }

        try {
            return mapper.readValue(response.body().byteStream(), this.getTypeRef());
        } catch (Exception e) {
            throw new EdcException(e);
        } finally {
            response.close();
        }
    }

    private TypeReference<List<TargetNode>> getTypeRef() {
        return new TypeReference<List<TargetNode>>() {};
    }
}
