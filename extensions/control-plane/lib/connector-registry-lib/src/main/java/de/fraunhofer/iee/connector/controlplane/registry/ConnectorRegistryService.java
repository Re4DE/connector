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

package de.fraunhofer.iee.connector.controlplane.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iee.connector.controlplane.registry.util.RequestBuilder;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.List;

import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusNot2xxOr4xx;

public class ConnectorRegistryService {
    private static final MediaType TYPE_JSON = MediaType.parse("application/json");

    private final Monitor monitor;
    private final EdcHttpClient client;
    private final ObjectMapper mapper;

    private final TypeReference<List<TargetNode>> targetNodeRef;

    private final String url;
    private final String apiKey;

    public ConnectorRegistryService(EdcHttpClient client, Monitor monitor, ObjectMapper mapper, String url, String apiKey) {
        this.client = client;
        this.monitor = monitor;
        this.mapper = mapper;
        this.url = url;
        this.apiKey = apiKey;

        this.targetNodeRef = new TypeReference<>() {};
    }

    public void registerConnector(String connectorName, String participantId, String dsp) {
        var body = RequestBuilder.buildPostRequestBody(connectorName, participantId, dsp);
        var req = RequestBuilder.buildPostRequest(this.url, this.apiKey, RequestBody.create(body.toString(), TYPE_JSON));
        try (var res = this.client.execute(req, List.of(retryWhenStatusNot2xxOr4xx()))) {
            if (!res.isSuccessful()) {
                // Connector is already registered, not need to act
                if (res.code() == 400) {
                    if (res.body().toString().contains("Connector already registered")) {
                        this.monitor.info("Connector is already registered in Connector Registry");
                        return;
                    }
                }

                this.monitor.debug("Could not register in Connector Registry, response not 200, but is: %s %s".formatted(res.code(), res.message()));
                this.monitor.debug("Response body: %s".formatted(res.body().string()));
                return;
            }
            this.monitor.info("Connector registration complete");
        } catch (Exception e) {
            throw new EdcException(e);
        }
    }

    public List<TargetNode> getAllConnectors() {
        var req = RequestBuilder.buildGetRequest(this.url, this.apiKey);
        try (var res = this.client.execute(req, List.of(retryWhenStatusNot2xxOr4xx()))) {
            if (!res.isSuccessful()) {
                this.monitor.debug("Failed pulling connectors from registry, response not 200, but is: %s %s".formatted(res.code(), res.message()));
                return List.of();
            }

            return this.mapper.readValue(res.body().string(), this.targetNodeRef);
        } catch (Exception e) {
            throw new EdcException(e);
        }
    }
}
