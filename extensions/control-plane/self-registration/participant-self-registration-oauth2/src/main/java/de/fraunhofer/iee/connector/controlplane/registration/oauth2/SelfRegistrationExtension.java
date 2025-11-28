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

package de.fraunhofer.iee.connector.controlplane.registration.oauth2;

import jakarta.json.Json;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.List;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusNot2xxOr4xx;

@Extension(value = "Participant self registration service for oauth2 identity provider")
public class SelfRegistrationExtension implements ServiceExtension {

    @Setting(key = "edc.participant.registration.enabled", description = "Switch to enable self registration at the endpoint of the connector registry", defaultValue = "true")
    private boolean enabled;

    @Setting(key = "edc.participant.registration.url", description = "The endpoint of the connector registry to perform the self registration")
    private String url;

    @Inject
    private EdcHttpClient httpclient;

    @Inject
    private IdentityService identityService;

    @Inject
    private DataspaceProfileContextRegistry dataspaceProfileContextRegistry;

    private static final MediaType TYPE_JSON = MediaType.parse("application/json");

    private final TokenParameters tokenParameters = TokenParameters.Builder.newInstance().build();

    private ServiceExtensionContext context;

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.context = context;
    }

    @Override
    public void start() {
        var monitor = this.context.getMonitor();
        if (this.enabled) {
            var dsp = ofNullable(this.dataspaceProfileContextRegistry.getWebhook("dataspace-protocol-http"))
                    .orElseThrow(() -> new EdcException("Dataspace protocol http protocol not found."));
            var requestBody = Json.createObjectBuilder()
                    .add("name", context.getParticipantId())
                    .add("url", dsp.url())
                    .add("supportedProtocols", Json.createArrayBuilder().add("dataspace-protocol-http"))
                    .build();
            var body = RequestBody.create(requestBody.toString(), TYPE_JSON);

            monitor.debug("Initiate self registration with connector registry.");

            this.identityService.obtainClientCredentials(this.tokenParameters)
                    .onSuccess(token -> {
                        var request = new Request.Builder()
                                .url(url)
                                .header("Authorization", "Bearer " + token.getToken())
                                .post(body)
                                .build();

                        this.httpclient.executeAsync(request, List.of(retryWhenStatusNot2xxOr4xx()))
                                .thenAccept(response -> {
                                    if (!response.isSuccessful()) {

                                        // Connector is already registered, no need to act
                                        if (response.code() == 400) {
                                            try {
                                                if (response.body() != null && response.body().string().contains("Connector already registered")) {
                                                    monitor.debug("Connector already registered. Self registration complete.");
                                                    response.close();
                                                    return;
                                                }
                                            } catch (Exception e) {
                                                response.close();
                                                throw new EdcException(e);
                                            }
                                        }

                                        monitor.debug("Self registration failed, response not 200, is: " + response.code() + " " + response.message());
                                        if (response.body() != null) {
                                            try {
                                                monitor.debug("Self registration response body: " + response.body().string());
                                            } catch (Exception e) {
                                                response.close();
                                                throw new EdcException(e);
                                            }
                                        }
                                        response.close();
                                        return;
                                    }

                                    monitor.debug("Self registration complete.");
                                    response.close();
                                });
                    })
                    .orElseThrow(failure -> new EdcException(format("Unable to obtain credentials: %s", failure.getFailureDetail())));
        } else {
            monitor.warning("Self Registration is deactivated, this connector will not appear in the data space catalog of other participants.");
        }
    }
}
