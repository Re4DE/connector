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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static de.fraunhofer.iee.connector.controlplane.registry.testfixtures.ConnectorRegistryTestUtil.connectorRegistryService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;

public class ConnectorRegistryServiceTest {

    @RegisterExtension
    static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final String participantId = "did:web:localhost:test";
    private final String dsp = "http://localhost:8080/dsp";
    private final String apiKey = "devpass";
    private final String connectorName = "tester";

    private final EdcHttpClient http = testHttpClient();
    private final TypeManager typeManager = new JacksonTypeManager();
    private ConnectorRegistryService client;

    @BeforeEach
    void setUp() {
        client = connectorRegistryService(http, typeManager, participantId, server.baseUrl(), apiKey);
    }

    @Nested
    class RegisterConnector {
        @Test
        void shouldSucceed_whenConnectorIsRegisteredSuccessful() {
            server.stubFor(post(anyUrl()).willReturn(okJson("")));
            client.registerConnector(connectorName, dsp);

            server.verify(postRequestedFor(urlEqualTo("/"))
                    .withHeader("x-api-key", equalTo(apiKey))
                    .withHeader("Content-Type", containing("application/json"))
                    .withRequestBody(matchingJsonPath("$.name", equalTo(connectorName)))
                    .withRequestBody(matchingJsonPath("$.id", equalTo(participantId)))
                    .withRequestBody(matchingJsonPath("$.url", equalTo(dsp)))
                    .withRequestBody(matchingJsonPath("$.supportedProtocols[0]", equalTo("dataspace-protocol-http"))));
        }

        @Test
        void shouldNotFail_whenConnectorAlreadyRegistered() {
            server.stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(400).withBody("Connector already registered")));
            client.registerConnector(connectorName, dsp);

            server.verify(postRequestedFor(urlEqualTo("/")));
        }

        @Test
        void shouldNotFail_whenAnotherBadRequest() {
            server.stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(400).withBody("Some other bad request")));
            client.registerConnector(connectorName, dsp);

            server.verify(postRequestedFor(urlEqualTo("/")));
        }

        @Test
        void shouldThrowException_whenServerRespondsWith500() {
            server.stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(500).withBody("Internal server error")));

            assertThatThrownBy(() -> client.registerConnector(connectorName, dsp))
                    .isInstanceOf(EdcException.class);
        }
    }

    @Nested
    class GetAllConnector {
        @Test
        void shouldReturnParsedConnector_whenIsSuccessful() {
            var resJson = """
                    [
                        {
                            "name": "tester",
                            "id": "did:web:localhost:test",
                            "url": "http://localhost:8080/dsp",
                            "supportedProtocols": ["dataspace-protocol-http"]
                        }
                    ]
                    """;
            server.stubFor(get(anyUrl()).willReturn(okJson(resJson)));

            var result = client.getAllConnectors();
            assertThat(result).hasSize(1);
            server.verify(getRequestedFor(urlPathEqualTo("/"))
                    .withQueryParam("participantId", equalTo(participantId))
                    .withHeader("x-api-key", equalTo(apiKey)));
        }

        @Test
        void shouldReturnEmptyList_whenNotSuccessful() {
            server.stubFor(get(anyUrl()).willReturn(aResponse().withStatus(400)));

            var result = client.getAllConnectors();
            assertThat(result).isEmpty();

            server.verify(getRequestedFor(urlPathEqualTo("/"))
                    .withQueryParam("participantId", equalTo(participantId))
                    .withHeader("x-api-key", equalTo(apiKey)));

        }

        @Test
        void shouldThrowsException_whenServerRespondsWith500() {
            server.stubFor(get(anyUrl())
                    .willReturn(aResponse().withStatus(500).withBody("Internal server error")));

            assertThatThrownBy(() -> client.getAllConnectors())
                    .isInstanceOf(EdcException.class);
        }

    }
}
