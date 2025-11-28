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

package de.fraunhofer.iee.connector.controlplane.registration.ssi;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Base64;
import java.util.List;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusNot2xxOr4xx;

@Extension(value = "Participant self registration service for ssi identity provider")
public class SelfRegistrationExtension implements ServiceExtension {

    private static final MediaType TYPE_JSON = MediaType.parse("application/json");
    private static final String CLIENT_CREDENTIALS = "client_credentials";
    private static final String PROTOCOL_ENDPOINT = "ProtocolEndpoint";

    @Setting(key = "edc.participant.registration.enabled", description = "Switch to enable self registration at the endpoint of the connector registry and the creation of a participant context in the identity hub", defaultValue = "true")
    private boolean enabledRegistry;

    @Setting(key = "edc.participant.registration.membership.issuance.enabled", description = "Switch to enable self issuance of a Membership Credential", defaultValue = "true")
    private boolean enabledIssuance;

    @Setting(key = "edc.participant.registration.url", description = "The endpoint of the connector registry to perform the self registration")
    private String registryUrl;

    @Setting(key = "edc.participant.registration.token.url", description = "The endpoint of the connector registry to perform the self registration")
    private String tokenUrl;

    @Setting(key = "edc.participant.registration.keys.name.overwrite", description = "Override the suffix name of all keys that will saved in the vault for this participant", required = false, warnOnMissingConfig = true)
    private String overwriteKey;

    @Setting(key = "edc.participant.registration.token.client.id", description = "The endpoint of the connector registry to perform the self registration")
    private String tokenClientId;

    @Setting(key = "edc.participant.registration.token.client.secret.alias", description = "The endpoint of the connector registry to perform the self registration")
    private String tokenClientSecretAlias;

    @Setting(key = "edc.participant.registration.ih.identity.url", description = "The endpoint of the identity hub identity api to perform participant context creation")
    private String ihIdenUrl;

    @Setting(key = "edc.participant.registration.ih.credentials.url", description = "The endpoint of the identity hub credential api to perform participant context creation")
    private String ihCredUrl;

    @Setting(key = "edc.participant.registration.issuer.did", description = "The did of the dataspace issuer")
    private String issuerDid;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private Oauth2Client oauth2Client;

    @Inject
    Vault vault;

    @Inject
    private DidResolverRegistry didResolverRegistry;

    @Inject
    private Hostname hostname;

    private ServiceExtensionContext context;
    private Monitor monitor;
    private String participantId;

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.context = context;
        this.monitor = context.getMonitor();
        this.participantId = context.getParticipantId();
    }

    @Override
    public void start() {
        if (this.enabledRegistry) {
            // Register the connector in the identity hub by creating a participant context
            this.registerInIdentityHub();

            // Register in connector registry to make this connector appear in the federated catalog
            this.registerInConnectorRegistry();
        } else {
            this.monitor.warning("Self Registration is deactivated, this connector will not appear in the data space catalog of other participants. Also a participant context in the identity hub for this connector need to be created.");
        }

        if (this.enabledIssuance) {
            // Obtain membership credential if not present in identity hub
            this.issueMembershipCredential();
        } else {
            this.monitor.warning("Self issuance of MembershipCredential is deactivated, this connector is not able to communicate with other participants in the data space as long as a Membership Credential is issued!");
        }
    }

    private void registerInConnectorRegistry() {

        var did = ofNullable(this.didResolverRegistry.resolve(this.participantId).getContent())
                .orElseThrow(() -> new EdcException("Could not resolve own did document."));
        var service = did.getService().stream()
                .filter(s -> s.getType().equalsIgnoreCase(PROTOCOL_ENDPOINT))
                .findFirst()
                .orElseThrow(() -> new EdcException("Could not resolve service endpoint from did document."));
        var dsp = ofNullable(service.getServiceEndpoint())
                .orElseThrow(() -> new EdcException("Dataspace protocol http protocol not found."));

        var tokenClientSecret = ofNullable(this.vault.resolveSecret(this.tokenClientSecretAlias))
                .orElseThrow(() -> new EdcException("Could not retrieve client secret for self registration, could not find: %s".formatted(this.tokenClientSecretAlias)));

        var requestBody = Json.createObjectBuilder()
                .add("name", this.participantId)
                .add("id", this.participantId)
                .add("url", dsp)
                .add("supportedProtocols", Json.createArrayBuilder().add("dataspace-protocol-http"))
                .build();
        var body = RequestBody.create(requestBody.toString(), TYPE_JSON);

        monitor.debug("Initiate self registration with connector registry.");

        this.oauth2Client.requestToken(SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                        .url(this.tokenUrl)
                        .grantType(CLIENT_CREDENTIALS)
                        .clientId(this.tokenClientId)
                        .clientSecret(tokenClientSecret)
                        .build())
                .onSuccess(token -> {
                    var request = new Request.Builder()
                            .url(this.registryUrl)
                            .header("Authorization", "Bearer " + token.getToken())
                            .post(body)
                            .build();

                    this.httpClient.executeAsync(request, List.of(retryWhenStatusNot2xxOr4xx()))
                            .thenAccept(response -> {
                                if (!response.isSuccessful()) {

                                    // Connector is already registered, no need to act
                                    if (response.code() == 400) {
                                        try {
                                            if (response.body() != null && response.body().string().contains("Connector already registered")) {
                                                this.monitor.info("Connector already registered in Connector Registry.");
                                                response.close();
                                                return;
                                            }
                                        } catch (Exception e) {
                                            response.close();
                                            throw new EdcException(e);
                                        }
                                    }

                                    this.monitor.debug("Self registration failed for Connector Registry, response not 200, is: " + response.code() + " " + response.message());
                                    if (response.body() != null) {
                                        try {
                                            this.monitor.debug("Self registration for Connector Registry, response body: " + response.body().string());
                                        } catch (Exception e) {
                                            response.close();
                                            throw new EdcException(e);
                                        }
                                    }
                                    response.close();
                                    return;
                                }

                                this.monitor.info("Self registration in Connector Registry complete.");
                                response.close();
                            });
                })
                .orElseThrow(failure -> new EdcException(format("Unable to obtain credentials: %s", failure.getFailureDetail())));
    }

    // Using the seed Super User to register this connector if there is no participant context is created
    // This setup is only possible, if this connector and its identity hub uses the same vault instance
    private void registerInIdentityHub() {
        var participantIdB64 = Base64.getEncoder().encodeToString(this.participantId.getBytes());

        var apiKey = ofNullable(this.vault.resolveSecret("super-user-apikey"))
                .orElseThrow(() -> new EdcException("Missing Super User api key in vault"));

        // Get the dsp address from settings or use the default http:// address
        var dsp = this.context.getSetting("edc.dsp.callback.address", "");
        if (dsp.isEmpty()) {
            var port = this.context.getSetting("web.http.protocol.port", "/api/protocol");
            var path = this.context.getSetting("web.http.protocol.path", "8282");
            dsp = "http://%s:%s%s".formatted(this.hostname.get(), port, path);
        }

        // Create participant context if missing
        try (var response = this.httpClient.execute(new Request.Builder()
                .url("%s/v1alpha/participants/%s".formatted(this.ihIdenUrl, participantIdB64))
                .addHeader("x-api-key", apiKey)
                .build(), List.of(retryWhenStatusNot2xxOr4xx()))) {

            if (response.isSuccessful()) {
                this.monitor.info("Participant context already created.");
            } else {
                var request = this.buildParticipantJsonDto(participantIdB64, dsp);
                try (var res = this.httpClient.execute(new Request.Builder()
                        .url("%s/v1alpha/participants".formatted(this.ihIdenUrl))
                        .addHeader("x-api-key", apiKey)
                        .post(RequestBody.create(request.toString(), TYPE_JSON))
                        .build(), List.of(retryWhenStatusNot2xxOr4xx()))) {

                    if (res.isSuccessful()) {
                        this.monitor.info("Successfully created participant context.");
                    } else {
                        this.monitor.warning("Failed to create participant context with error: %s - %s".formatted(res.code(), res.message()));
                    }
                } catch (Exception e) {
                    throw new EdcException("Could not create participant context.", e);
                }
            }
        } catch (Exception e) {
            throw new EdcException("Could not fetch participant context.", e);
        }
    }

    private JsonObject buildParticipantJsonDto(String participantIdB64, String dsp) {
        var keySuffix = this.overwriteKey.isEmpty() ? this.participantId : this.overwriteKey;

        var json = Json.createObjectBuilder()
                .add("active", true)
                .add("participantId", this.participantId)
                .add("did", this.participantId)
                .add("roles", Json.createArrayBuilder())
                .add("serviceEndpoints", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("type", "CredentialService")
                                .add("serviceEndpoint", "%s/v1/participants/%s".formatted(this.ihCredUrl, participantIdB64))
                                .add("id", "%s-credentialservice-1".formatted(this.participantId)))
                        .add(Json.createObjectBuilder()
                                .add("type", PROTOCOL_ENDPOINT)
                                .add("serviceEndpoint", dsp)
                                .add("id", "%s-dsp".formatted(this.participantId))))
                .add("key", Json.createObjectBuilder()
                        .add("keyId", "%s#key-1".formatted(this.participantId))
                        .add("privateKeyAlias", "%s#key-1".formatted(keySuffix))
                        .add("keyGeneratorParams", Json.createObjectBuilder()
                                .add("algorithm", "EC")));

        // add key override for sts account keys to json object if override is defined
        if (!this.overwriteKey.isEmpty()) {
            json.add("additionalProperties", Json.createObjectBuilder()
                    .add("clientSecret", "%s-sts-client-secret".formatted(keySuffix))
            );
        }

        return json.build();
    }

    // After successfully creating a participant context, issue the membership credential (if it is not present)
    private void issueMembershipCredential() {
        var participantIdB64 = Base64.getEncoder().encodeToString(this.participantId.getBytes());

        var apiKey = ofNullable(this.vault.resolveSecret("super-user-apikey"))
                .orElseThrow(() -> new EdcException("Missing Super User api key in vault."));

        // Request MembershipCredential if not present
        if (this.hasMembershipCredentials(participantIdB64, apiKey)) {
            this.monitor.info("Membership credential already issued.");
            return;
        }

        // Start a membership credential issuance process
        var request = this.buildMembershipCredentialJsonDto();
        try (var response = this.httpClient.execute(new Request.Builder()
                .url("%s/v1alpha/participants/%s/credentials/request".formatted(this.ihIdenUrl, participantIdB64))
                .addHeader("x-api-key", apiKey)
                .post(RequestBody.create(request.toString(), TYPE_JSON))
                .build(), List.of(retryWhenStatusNot2xxOr4xx()))) {
            if (response.isSuccessful()) {
                // Check whether the credentials were issued correctly, try three times, through warning of not successful
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(1000);
                    if (this.hasMembershipCredentials(participantIdB64, apiKey)) {
                        this.monitor.info("Membership credential successful issued.");
                        return;
                    }
                }
                this.monitor.warning("Membership credential issuance failed.");
            } else {
                this.monitor.warning("Membership credential request failed with error: %s - %s".formatted(response.code(), response.message()));
            }
        } catch (Exception e) {
            throw new EdcException("Could not start membership credential request.", e);
        }
    }

    private boolean hasMembershipCredentials(String participantIdB64, String apiKey) {
        // Try to query membership credentials
        try (var response = this.httpClient.execute(new Request.Builder()
                .url("%s/v1alpha/participants/%s/credentials?type=MembershipCredential".formatted(this.ihIdenUrl, participantIdB64))
                .addHeader("x-api-key", apiKey)
                .build(), List.of(retryWhenStatusNot2xxOr4xx()))) {
            if (response.isSuccessful()) {
                return response.body() != null && !response.body().string().equals("[]");
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new EdcException("Could not query membership credentials.", e);
        }
    }

    private JsonObject buildMembershipCredentialJsonDto() {
        return Json.createObjectBuilder()
                .add("issuerDid", this.issuerDid)
                .add("credentials", Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                        .add("id", "membership-credential-def-1")
                        .add("type", "MembershipCredential")
                        .add("format", "VC1_0_JWT"))
                )
                .build();
    }
}
