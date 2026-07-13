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
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static de.fraunhofer.iee.connector.controlplane.registry.testfixtures.ConnectorRegistryTestUtil.connectorRegistryService;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;

public class ConnectorRegistryServiceTest {

    @RegisterExtension
    static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final String participantId = "did:web:localhost:test";
    private final String url = "http://localhost";
    private final String apiKey = "devpass";

    private final EdcHttpClient http = testHttpClient();
    private final TypeManager typeManager = new JacksonTypeManager();
    private final ConnectorRegistryService client = connectorRegistryService(http, typeManager, participantId, url, apiKey);

    @Nested
    class Execute {
        @Test
        void shouldSucceed_whenConnectorIsRegisteredSuccessful() {
            server.stubFor(post(anyUrl()).willReturn(okJson("")));

        }
    }
}
