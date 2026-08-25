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
import static java.util.Map.entry;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.eclipse.edc.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class RegisterIdentityHubTest {

    @RegisterExtension
    static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String PARTICIPANT_ID = "did:web:localhost:test";
    private static final String API_KEY = "devpass";
    private static final String DSP = "http://localhost:8080/dsp";
    private static final String CONNECTOR_NAME = "tester";
    private static final String SUPER_USER_API_KEY = "super-secret";
    private final EdcHttpClient http = testHttpClient();
    private final DspBaseWebhookAddress dspBaseWebhookAddress = mock();
    private final Vault vault = mock();
    private final Monitor monitor = mock();

    private SelfRegistrationExtension extension;
    private String identityHubPath;
    private String identityHubRequestPath;
    private ServiceExtensionContext context;
    private ObjectFactory factory;
    private Map<String, String> configMap;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        this.context = context;
        this.factory = factory;

        var participantIdB64 = Base64.getUrlEncoder().encodeToString(PARTICIPANT_ID.getBytes());
        identityHubPath = "/api/identity/v1alpha/participants/%s".formatted(participantIdB64);
        identityHubRequestPath = "/api/identity/v1alpha/participants";

        context.registerService(DspBaseWebhookAddress.class, dspBaseWebhookAddress);
        context.registerService(Vault.class, vault);
        context.registerService(EdcHttpClient.class, http);

        when(dspBaseWebhookAddress.get()).thenReturn(DSP);
        when(vault.resolveSecret(anyString())).thenReturn(SUPER_USER_API_KEY);
        when(context.getParticipantId()).thenReturn(PARTICIPANT_ID);
        when(context.getMonitor()).thenReturn(monitor);

        configMap = baseConfigMap();
        configMap.put("edc.registration.participant.context.enabled", "true");
    }

    @AfterEach
    void tearDown() {
        if (extension != null) {
            extension.shutdown();
        }
        server.resetAll();
    }

    @Test
    void shouldNotRegister_whenParticipantContextAlreadyExists() {
        server.stubFor(get(urlPathEqualTo(identityHubPath))
                .willReturn(okJson("[]")));

        when(context.getConfig()).thenReturn(configWith(configMap));
        extension = factory.constructInstance(SelfRegistrationExtension.class);
        extension.initialize(context);
        extension.start();

        server.verify(1, getRequestedFor(urlPathEqualTo(identityHubPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));
        server.verify(0, postRequestedFor(urlPathEqualTo(identityHubRequestPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));
    }

    @Test
    void shouldRegister_whenParticipantContextDoesNotExist() {
        server.stubFor(get(urlPathEqualTo(identityHubPath))
                .willReturn(aResponse().withStatus(404)));

        server.stubFor(post(urlPathEqualTo(identityHubRequestPath))
                .willReturn(okJson("{}")));

        when(context.getConfig()).thenReturn(configWith(configMap));
        extension = factory.constructInstance(SelfRegistrationExtension.class);
        extension.initialize(context);
        extension.start();

        var base64ParticipantId = Base64.getEncoder().encodeToString(PARTICIPANT_ID.getBytes());
        var expectedJson = """
                {
                    "active": true,
                    "participantId": "%1$s",
                    "did": "%1$s",
                    "roles": [],
                    "serviceEndpoints": [
                        {
                            "type": "CredentialService",
                            "serviceEndpoint": "%2$s/v1/participants/%3$s",
                            "id": "%1$s-credentialservice-1"
                        },
                        {
                            "type": "ProtocolEndpoint",
                            "serviceEndpoint": "%4$s",
                            "id": "%1$s-dsp"
                        }
                    ],
                    "key": {
                        "keyId": "%1$s#key-1",
                        "privateKeyAlias": "%1$s#key-1",
                        "keyGeneratorParams": {
                            "algorithm": "EC"
                        }
                    }
                }
                """.formatted(PARTICIPANT_ID, server.baseUrl() + "/api/credentials", base64ParticipantId, DSP);

        server.verify(1, getRequestedFor(urlPathEqualTo(identityHubPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));

        server.verify(1, postRequestedFor(urlPathEqualTo(identityHubRequestPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson(expectedJson)));
    }

    @Test
    void shouldRegisterWithDifferentKeySuffix_whenParticipantContextDoesNotExist() {
        server.stubFor(get(urlPathEqualTo(identityHubPath))
                .willReturn(aResponse().withStatus(404)));

        server.stubFor(post(urlPathEqualTo(identityHubRequestPath))
                .willReturn(okJson("{}")));

        configMap.put("edc.registration.keys.name.overwrite", "short-name");
        when(context.getConfig()).thenReturn(configWith(configMap));
        extension = factory.constructInstance(SelfRegistrationExtension.class);
        extension.initialize(context);
        extension.start();

        var base64ParticipantId = Base64.getEncoder().encodeToString(PARTICIPANT_ID.getBytes());
        var expectedJson = """
                {
                    "active": true,
                    "participantId": "%1$s",
                    "did": "%1$s",
                    "roles": [],
                    "serviceEndpoints": [
                        {
                            "type": "CredentialService",
                            "serviceEndpoint": "%2$s/v1/participants/%3$s",
                            "id": "%1$s-credentialservice-1"
                        },
                        {
                            "type": "ProtocolEndpoint",
                            "serviceEndpoint": "%4$s",
                            "id": "%1$s-dsp"
                        }
                    ],
                    "key": {
                        "keyId": "%1$s#key-1",
                        "privateKeyAlias": "%5$s#key-1",
                        "keyGeneratorParams": {
                            "algorithm": "EC"
                        }
                    },
                    "additionalProperties": {
                        "clientSecret": "%5$s-sts-client-secret"
                    }
                }
                """.formatted(PARTICIPANT_ID, server.baseUrl() + "/api/credentials", base64ParticipantId, DSP, "short-name");

        server.verify(1, getRequestedFor(urlPathEqualTo(identityHubPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));

        server.verify(1, postRequestedFor(urlPathEqualTo(identityHubRequestPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson(expectedJson)));
    }

    @Test
    void shouldThrow_whenGetParticipantContextFails() {
        server.stubFor(get(urlPathEqualTo(identityHubPath))
                .willReturn(aResponse().withStatus(500)));

        when(context.getConfig()).thenReturn(configWith(configMap));
        extension = factory.constructInstance(SelfRegistrationExtension.class);
        extension.initialize(context);

        assertThatThrownBy(() -> extension.start())
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Could not fetch participant context.");

        server.verify(moreThanOrExactly(1), getRequestedFor(urlPathEqualTo(identityHubPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));
        server.verify(0, postRequestedFor(urlPathEqualTo(identityHubRequestPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));
    }

    @Test
    void shouldThrow_whenCreateParticipantContextFails() {
        server.stubFor(get(urlPathEqualTo(identityHubPath))
                .willReturn(aResponse().withStatus(404)));

        server.stubFor(post(urlPathEqualTo(identityHubRequestPath))
                .willReturn(aResponse().withStatus(500)));

        when(context.getConfig()).thenReturn(configWith(configMap));
        extension = factory.constructInstance(SelfRegistrationExtension.class);
        extension.initialize(context);

        assertThatThrownBy(() -> extension.start())
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Could not create participant context.");

        server.verify(1, getRequestedFor(urlPathEqualTo(identityHubPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));

        server.verify(moreThanOrExactly(1), postRequestedFor(urlPathEqualTo(identityHubRequestPath))
                .withHeader("x-api-key", equalTo(SUPER_USER_API_KEY)));
    }

    @Test
    void shouldThrow_whenApiKeyMissing() {
        when(vault.resolveSecret(anyString())).thenReturn(null);

        when(context.getConfig()).thenReturn(configWith(configMap));
        extension = factory.constructInstance(SelfRegistrationExtension.class);
        extension.initialize(context);

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
