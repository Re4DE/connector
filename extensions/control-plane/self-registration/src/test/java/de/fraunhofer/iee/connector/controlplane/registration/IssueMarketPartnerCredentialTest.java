package de.fraunhofer.iee.connector.controlplane.registration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.Map.entry;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class IssueMarketPartnerCredentialTest {
    @RegisterExtension
    static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String CREDENTIAL_TYPE = "MarketPartnerCredential";
    private static final String PARTICIPANT_ID = "did:web:localhost:test";
    private static final String DSP = "http://localhost:8080/dsp";
    private static final String API_KEY = "devpass";
    private static final String CONNECTOR_NAME = "tester";
    private static final String SUPER_USER_API_KEY = "super-secret";
    private final EdcHttpClient http = testHttpClient();
    private final DspBaseWebhookAddress dspBaseWebhookAddress = mock();
    private final Vault vault = mock();
    private final Monitor monitor = mock();

    private SelfRegistrationExtension extension;
    private String credentialPath;
    private String requestPath;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        var participantIdB64 = Base64.getUrlEncoder().encodeToString(PARTICIPANT_ID.getBytes());
        credentialPath = "/api/identity/v1alpha/participants/%s/credentials".formatted(participantIdB64);
        requestPath = credentialPath + "/request";

        context.registerService(DspBaseWebhookAddress.class, dspBaseWebhookAddress);
        context.registerService(Vault.class, vault);
        context.registerService(EdcHttpClient.class, http);

        when(dspBaseWebhookAddress.get()).thenReturn(DSP);
        when(vault.resolveSecret(anyString())).thenReturn(SUPER_USER_API_KEY);
        when(context.getMonitor()).thenReturn(monitor);
        when(context.getParticipantId()).thenReturn(PARTICIPANT_ID);

        Map<String, String> configMap = baseConfigMap();
        configMap.put("edc.registration.marketpartner.issuance.enabled", "true");
        var config = configWith(configMap);
        when(context.getConfig()).thenReturn(config);

        extension = factory.constructInstance(SelfRegistrationExtension.class);
        extension.initialize(context);
    }

    @AfterEach
    void tearDown() {
        if (extension != null) {
            extension.shutdown();
        }
        server.resetAll();
    }

    @Test
    void shouldNotRequest_whenCredentialAlreadyIssued() {
        var resJson = """
                [
                    {"id":"credential-1","type":"MarketPartnerCredential"}
                ]
                """;
        server.stubFor(get(urlPathEqualTo(credentialPath))
                .withQueryParam("type", equalTo(CREDENTIAL_TYPE))
                .willReturn(okJson(resJson)));

        extension.start();

        server.verify(1, getRequestedFor(urlPathEqualTo(credentialPath))
                .withQueryParam("type", equalTo(CREDENTIAL_TYPE))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));
        server.verify(0, postRequestedFor(urlPathEqualTo(requestPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));
    }

    @Test
    void shouldIssueCredential_whenNotPresent() {
        var resJson = """
                [
                    {"id":"credential-1","type":"MarketPartnerCredential"}
                ]
                """;
        server.stubFor(get(urlPathEqualTo(credentialPath))
                .withQueryParam("type", equalTo(CREDENTIAL_TYPE))
                .inScenario("Market Partner Credential Issuance")
                .whenScenarioStateIs(STARTED)
                .willReturn(okJson("[]"))
                .willSetStateTo("Issuing Credential"));

        server.stubFor(post(urlPathEqualTo(requestPath))
                .inScenario("Market Partner Credential Issuance")
                .whenScenarioStateIs("Issuing Credential")
                .willReturn(okJson("[]")).willSetStateTo("Requesting Credential"));

        server.stubFor(get(urlPathEqualTo(credentialPath))
                .withQueryParam("type", equalTo(CREDENTIAL_TYPE))
                .inScenario("Market Partner Credential Issuance")
                .whenScenarioStateIs("Requesting Credential")
                .willReturn(okJson(resJson)));

        extension.start();

        server.verify(1, postRequestedFor(urlPathEqualTo(requestPath)).withHeader("x-api-key", equalTo(SUPER_USER_API_KEY))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.issuerDid", equalTo("did:web:issuer")))
                .withRequestBody(matchingJsonPath("$.credentials[0].id", equalTo("marketpartner-credential-def-1")))
                .withRequestBody(matchingJsonPath("$.credentials[0].type", equalTo("MarketPartnerCredential")))
                .withRequestBody(matchingJsonPath("$.credentials[0].format", equalTo("VC1_0_JWT"))));

        server.verify(2, getRequestedFor(urlPathEqualTo(credentialPath))
                .withQueryParam("type", equalTo(CREDENTIAL_TYPE))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));
    }

    @Test
    void shouldRetryIssuance_whenRequestFails() {
        server.stubFor(get(urlPathEqualTo(credentialPath))
                .withQueryParam("type", equalTo(CREDENTIAL_TYPE))
                .willReturn(okJson("[]")));

        server.stubFor(post(urlPathEqualTo(requestPath))
                .willReturn(okJson("[]")));

        extension.start();

        server.verify(1, postRequestedFor(urlPathEqualTo(requestPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));

        server.verify(moreThanOrExactly(1), getRequestedFor(urlPathEqualTo(credentialPath))
                .withQueryParam("type", equalTo(CREDENTIAL_TYPE))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));
    }


    @Test
    void shouldThrow_whenCheckForCredentialFails() {
        server.stubFor(get(urlPathEqualTo(credentialPath))
                .withQueryParam("type", equalTo(CREDENTIAL_TYPE))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> extension.start())
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Could not query credentials of type: %s".formatted(CREDENTIAL_TYPE));

        server.verify(moreThanOrExactly(1), getRequestedFor(urlPathEqualTo(credentialPath))
                .withQueryParam("type", equalTo(CREDENTIAL_TYPE))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));

        server.verify(0, postRequestedFor(urlPathEqualTo(requestPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));
    }

    @Test
    void shouldThrow_whenCredentialIssuanceFails() {
        server.stubFor(get(urlPathEqualTo(credentialPath))
                .withQueryParam("type", equalTo(CREDENTIAL_TYPE))
                .willReturn(okJson("[]")));

        server.stubFor(post(urlPathEqualTo(requestPath))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> extension.start())
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Could not start market partner credential request.");

        server.verify(1, getRequestedFor(urlPathEqualTo(credentialPath))
                .withQueryParam("type", equalTo(CREDENTIAL_TYPE))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));

        server.verify(moreThanOrExactly(1), postRequestedFor(urlPathEqualTo(requestPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));
    }

    @Test
    void shouldThrow_whenCheckForCredentialAfterNotPresentFails() {
        server.stubFor(get(urlPathEqualTo(credentialPath))
                .withQueryParam("type", equalTo(CREDENTIAL_TYPE))
                .inScenario("Market Partner Credential Issuance")
                .whenScenarioStateIs(STARTED)
                .willReturn(okJson("[]"))
                .willSetStateTo("Issuing Credential"));

        server.stubFor(post(urlPathEqualTo(requestPath))
                .inScenario("Market Partner Credential Issuance")
                .whenScenarioStateIs("Issuing Credential")
                .willReturn(okJson("[]")).willSetStateTo("Requesting Credential"));

        server.stubFor(get(urlPathEqualTo(credentialPath))
                .withQueryParam("type", equalTo(CREDENTIAL_TYPE))
                .inScenario("Market Partner Credential Issuance")
                .whenScenarioStateIs("Requesting Credential")
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> extension.start())
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Could not start market partner credential request.")
                .hasCauseInstanceOf(EdcException.class).cause()
                .hasMessageContaining("Could not query credentials of type: %s".formatted(CREDENTIAL_TYPE));

        server.verify(1, postRequestedFor(urlPathEqualTo(requestPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));

        server.verify(moreThanOrExactly(1), getRequestedFor(urlPathEqualTo(credentialPath))
                .withQueryParam("type", equalTo(CREDENTIAL_TYPE))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));
    }

    @Test
    void shouldThrow_whenApiKeyMissing() {
        when(vault.resolveSecret(anyString())).thenReturn(null);

        assertThatThrownBy(() -> extension.start())
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Missing Super User api key in vault.");

        server.verify(0, anyRequestedFor(anyUrl()));
    }

    private Config configWith(Map<String, String> configMap) {
        return ConfigFactory.fromMap(configMap);
    }

    private Map<String, String> baseConfigMap() {
        var base = server.baseUrl();
        return new HashMap<>(Map.ofEntries(
                entry("edc.registration.registry.enabled", "false"),
                entry("edc.registration.participant.context.enabled", "false"),
                entry("edc.registration.membership.issuance.enabled", "false"),
                entry("edc.registration.marketpartner.issuance.enabled", "false"),
                entry("edc.registration.registry.url", base + "/api/registry"),
                entry("edc.registration.registry.api.key", API_KEY),
                entry("edc.registration.ih.identity.url", base + "/api/identity"),
                entry("edc.registration.ih.credentials.url", base + "/api/credentials"),
                entry("edc.registration.issuer.did", "did:web:issuer"),
                entry("edc.registration.connector.name", CONNECTOR_NAME),
                entry("edc.participant.id", PARTICIPANT_ID)
        ));
    }
}
