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

package de.fraunhofer.iee.connector.controlplane.fc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.List;

import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusNot2xxOr4xx;

public class CatalogNodeDirectory implements TargetNodeDirectory {

    private final static String X_API_KEY = "x-api-key";
    private final static String GET = "GET";

    private final EdcHttpClient client;
    private final Monitor monitor;
    private final String url;
    private final String apiKey;
    private final ObjectMapper mapper;

    public CatalogNodeDirectory(EdcHttpClient client, String apiKey, Monitor monitor, String url, ObjectMapper mapper) {
        this.client = client;
        this.apiKey = apiKey;
        this.monitor = monitor;
        this.url = url;
        this.mapper = mapper;
    }

    // Not implemented, the external connector registry gives a list of all available participants
    @Override
    public void insert(TargetNode targetNode) {}

    @Override
    public List<TargetNode> getAll() {
        this.monitor.debug("Getting all catalog nodes");

        try {
            var request = new Request.Builder()
                    .url(url)
                    .header(X_API_KEY, this.apiKey)
                    .method(GET, null)
                    .build();

            return this.client.executeAsync(request, List.of(retryWhenStatusNot2xxOr4xx()))
                    .thenApply(this::handleResponse)
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
