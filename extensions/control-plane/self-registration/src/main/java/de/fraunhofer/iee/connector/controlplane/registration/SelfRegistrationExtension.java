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

package de.fraunhofer.iee.connector.controlplane.registration;

import de.fraunhofer.iee.connector.controlplane.registration.util.RequestBuilder;
import de.fraunhofer.iee.connector.controlplane.registry.ConnectorRegistryService;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress;
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

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusNot2xxOr4xx;

@Extension(value = "Participant self registration service")
public class SelfRegistrationExtension implements ServiceExtension {
    private final static String MEMBERSHIP_CREDENTIAL_TYPE = "MembershipCredential";
    private final static String MARKET_PARTNER_CREDENTIAL_TYPE = "MarketPartnerCredential";
    private final static String SUPER_USER_APIKEY = "super-user-apikey";

    @Setting(key = "edc.registration.registry.enabled", description = "Switch to activate/deactivate the self registration in the connector registry. If deactivated, this connector will not appear in the data space catalog of other participants", defaultValue = "true")
    private boolean enabledRegistry;

    @Setting(key = "edc.registration.participant.context.enabled", description = "Switch to activate/deactivate the creation of a participant context in the Identity Hub. If deactivated, this connector can not use SSI", defaultValue = "true")
    private boolean enabledParticipantContext;

    @Setting(key = "edc.registration.membership.issuance.enabled", description = "Switch to enable self issuance of a Membership Credential. If deactivated, this connector can not communicate with other participants with SSI. Membership Credential need to be issued manual", defaultValue = "true")
    private boolean enabledIssuance;

    @Setting(key = "edc.registration.marketPartner.issuance.enabled", description = "Switch to enable self issuance of a Market Partner Credential. Default is false, if activate this participant is part of the regulated market communication", defaultValue = "false")
    private boolean enableMarketPartner;

    @Setting(key = "edc.registration.connector.name", description = "A human readable name of the connector, if not used the participant id is then used", required = false)
    private String connectorName;

    @Setting(key = "edc.registration.registry.url", description = "The url of the deployed Connector Registry")
    private String registryUrl;

    @Setting(key = "edc.registration.registry.api.key", description = "The api key needed to authenticated at the Connector Registry")
    private String apiKey;

    @Setting(key = "edc.registration.keys.name.overwrite", description = "Override the suffix name of all keys that will saved in the vault for this participant", required = false, warnOnMissingConfig = true)
    private String overwriteKey;

    @Setting(key = "edc.registration.ih.identity.url", description = "The endpoint of the identity hub identity api to perform participant context creation")
    private String ihIdenUrl;

    @Setting(key = "edc.registration.ih.credentials.url", description = "The endpoint of the identity hub credential api to perform to issue a MembershipCredential")
    private String ihCredUrl;

    @Setting(key = "edc.registration.issuer.did", description = "The did of the Dataspace issuer")
    private String issuerDid;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    DspBaseWebhookAddress dspBaseWebhookAddress;

    @Inject
    Vault vault;

    @Inject
    private Hostname hostname;

    private ConnectorRegistryService registryService;

    private Monitor monitor;
    private String participantId;
    private String participantIdB64;

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.monitor = context.getMonitor();
        this.participantId = context.getParticipantId();
        this.participantIdB64 = Base64.getEncoder().encodeToString(this.participantId.getBytes());

        this.registryService = new ConnectorRegistryService(this.httpClient, this.monitor, null, this.registryUrl, this.apiKey);
        this.connectorName = this.connectorName == null ? this.participantId : this.connectorName;
    }

    @Override
    public void start() {
        if (this.enabledRegistry) {
            // Register in connector registry to make this connector appear in the federated catalog
            this.registerInConnectorRegistry();
        } else {
            this.monitor.warning("Self Registration is deactivated, this connector will not appear in the Dataspace catalog of other participants. Also a participant context in the identity hub for this connector need to be created.");
        }

        if (this.enabledParticipantContext) {
            // Register the connector in the identity hub by creating a participant context
            this.registerInIdentityHub();
        } else {
            this.monitor.warning("Creation of a participant context is deactivated, make sure you use OAuth2 as Identity Service. Otherwise the connector is not able to issue a Membership Credential.");
        }

        if (this.enabledIssuance) {
            // Obtain membership credential if not present in identity hub
            this.issueMembershipCredential();
        } else {
            this.monitor.warning("Self issuance of MembershipCredential is deactivated, this connector is not able to communicate with other participants in the Dataspace as long as a Membership Credential is issued!");
        }

        if (this.enableMarketPartner) {
            // Obtain market partner credential if not present in identity hub
            this.issueMarketPartnerCredential();
        } else {
            this.monitor.info("Self issuance of MarketPartnerCredential is deactivated.");
        }
    }

    private void registerInConnectorRegistry() {
        this.monitor.debug("Initiate self registration with Connector Registry.");
        this.registryService.registerConnector(connectorName, this.participantId, this.dspBaseWebhookAddress.get());
    }

    // Using the seed Super User to register this connector with a participant context in the Identity Hub
    // This setup is only possible, if this connector and its Identity Hub uses the same Vault instance!!
    private void registerInIdentityHub() {
        var superApiKey = ofNullable(this.vault.resolveSecret(SUPER_USER_APIKEY))
                .orElseThrow(() -> new EdcException("Missing Super User api key in vault."));
        var dsp = this.dspBaseWebhookAddress.get();
        var keySuffix = this.overwriteKey == null ? this.participantId : this.overwriteKey;

        // Check whether the participant context is already created
        var reqGet = RequestBuilder.buildGetParticipantContextRequest(this.ihIdenUrl, this.participantIdB64, superApiKey);
        try (var res = this.httpClient.execute(reqGet, List.of(retryWhenStatusNot2xxOr4xx()))) {
            if (res.isSuccessful()) {
                this.monitor.info("Participant context already created.");
                return;
            }
        } catch (Exception e) {
            throw new EdcException("Could not fetch participant context.", e);
        }

        // Create participant context
        var reqPost = RequestBuilder.buildCreateParticipantContextRequest(this.ihIdenUrl, this.ihCredUrl, this.participantId, this.participantIdB64, superApiKey, dsp, keySuffix);
        try (var res = this.httpClient.execute(reqPost, List.of(retryWhenStatusNot2xxOr4xx()))) {
            if (res.isSuccessful()) {
                this.monitor.info("Successfully created participant context.");
            } else {
                this.monitor.warning("Failed to create participant context with error: %s - %s.".formatted(res.code(), res.message()));
            }
        } catch (Exception e) {
            throw new EdcException("Could not create participant context.", e);
        }
    }

    // After successfully creating a participant context, issue the membership credential (if it is not present)
    private void issueMembershipCredential() {
        var superApiKey = ofNullable(this.vault.resolveSecret(SUPER_USER_APIKEY))
                .orElseThrow(() -> new EdcException("Missing Super User api key in vault."));

        // Check whether the membership credential is already issued
        if (this.hasCredentialType(this.participantIdB64, superApiKey, MEMBERSHIP_CREDENTIAL_TYPE)) {
            this.monitor.info("Membership credential already issued.");
            return;
        }

        // Start a membership credential issuance process
        var reqPost = RequestBuilder.buildCreateMembershipCredentialRequest(this.ihIdenUrl, this.participantIdB64, superApiKey, this.issuerDid);
        try (var response = this.httpClient.execute(reqPost, List.of(retryWhenStatusNot2xxOr4xx()))) {
            if (response.isSuccessful()) {
                // Check whether the credentials were issued correctly, try three times, through warning of not successful
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(1000);
                    if (this.hasCredentialType(this.participantIdB64, superApiKey, MEMBERSHIP_CREDENTIAL_TYPE)) {
                        this.monitor.info("Membership credential successful issued.");
                        return;
                    }
                }
                this.monitor.warning("Membership credential issuance failed.");
            } else {
                this.monitor.warning("Membership credential request failed with error: %s - %s.".formatted(response.code(), response.message()));
            }
        } catch (Exception e) {
            throw new EdcException("Could not start membership credential request.", e);
        }
    }

    // After successfully creating a participant context, issue the market partner credential (if it is not present)
    private void issueMarketPartnerCredential() {
        var superApiKey = ofNullable(this.vault.resolveSecret(SUPER_USER_APIKEY))
                .orElseThrow(() -> new EdcException("Missing Super User api key in vault."));

        // Check whether the market partner credential is already issued
        if (this.hasCredentialType(this.participantIdB64, superApiKey, MARKET_PARTNER_CREDENTIAL_TYPE)) {
            this.monitor.info("Market Partner credential already issued.");
            return;
        }

        // Start a market partner credential issuance process
        var reqPost = RequestBuilder.buildCreateMarketPartnerCredentialRequest(this.ihIdenUrl, this.participantIdB64, superApiKey, this.issuerDid);
        try (var response = this.httpClient.execute(reqPost, List.of(retryWhenStatusNot2xxOr4xx()))) {
            if (response.isSuccessful()) {
                // Check whether the credentials were issued correctly, try three times, through warning of not successful
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(1000);
                    if (this.hasCredentialType(this.participantIdB64, superApiKey, MARKET_PARTNER_CREDENTIAL_TYPE)) {
                        this.monitor.info("Market partner credential successful issued.");
                        return;
                    }
                }
                this.monitor.warning("Market partner credential issuance failed.");
            } else {
                this.monitor.warning("Market partner credential request failed with error: %s - %s.".formatted(response.code(), response.message()));
            }
        } catch (Exception e) {
            throw new EdcException("Could not start market partner credential request.", e);
        }
    }

    private boolean hasCredentialType(String participantIdB64, String superApiKey, String credentialType) {
        var reqGet = RequestBuilder.buildGetCredentialTypeRequest(this.ihIdenUrl, participantIdB64, superApiKey, credentialType);
        try (var res = this.httpClient.execute(reqGet, List.of(retryWhenStatusNot2xxOr4xx()))) {
            if (res.isSuccessful()) {
                return !res.body().string().equals("[]");
            }
            return false;
        } catch (Exception e) {
            throw new EdcException("Could not query credentials of type: %s".formatted(credentialType), e);
        }
    }
}
