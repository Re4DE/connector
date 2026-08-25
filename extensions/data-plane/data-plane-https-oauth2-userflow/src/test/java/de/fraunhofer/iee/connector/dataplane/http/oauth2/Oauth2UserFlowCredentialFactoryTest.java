package de.fraunhofer.iee.connector.dataplane.http.oauth2;

import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Oauth2UserFlowCredentialFactoryTest {

    private static final String SECRET_NAME = "secretName";
    private static final String RESOLVED_PASSWORD = "resolvedPassword";
    private static final String TOKEN_URL_VALUE = "https://localhost:8000/token";
    private static final String USERNAME_VALUE = "user";

    private final Vault vault = mock();
    private final DataAddress dataAddress = mock();

    @Test
    void shouldCreateCredentialsRequest_whenPasswordIsResolved() {
        var factory = new Oauth2UserFlowCredentialFactory(vault);
        when(dataAddress.getStringProperty("oauth2:passwordSecretName")).thenReturn(SECRET_NAME);
        when(vault.resolveSecret(SECRET_NAME)).thenReturn(RESOLVED_PASSWORD);
        when(dataAddress.getStringProperty("oauth2:tokenUrl")).thenReturn(TOKEN_URL_VALUE);
        when(dataAddress.getStringProperty("oauth2:username")).thenReturn(USERNAME_VALUE);

        var result = factory.create(dataAddress);
        assertTrue(result.succeeded());

        var request = result.getContent();
        assertNotNull(request);

        assertAll(
                () -> assertEquals("https://localhost:8000/token", request.getUrl()),
                () -> assertEquals("password", request.getGrantType()),
                () -> assertEquals("user", request.getUsername()),
                () -> assertEquals("resolvedPassword", request.getPassword())
        );

        verify(vault).resolveSecret(SECRET_NAME);
    }

    @Test
    void shouldReturnFailure_whenPasswordCannotBeResolved() {
        var factory = new Oauth2UserFlowCredentialFactory(vault);
        when(dataAddress.getStringProperty("oauth2:passwordSecretName")).thenReturn(SECRET_NAME);
        when(vault.resolveSecret(SECRET_NAME)).thenReturn(null);

        var result = factory.create(dataAddress);
        assertTrue(result.failed());
        assertEquals("Cannot resolve password from the vault: " + SECRET_NAME, result.getFailureDetail());

        verify(vault).resolveSecret(SECRET_NAME);
    }

    @Test
    void shouldReturnFailure_whenPasswordSecretNameIsMissing() {
        var factory = new Oauth2UserFlowCredentialFactory(vault);
        when(dataAddress.getStringProperty("oauth2:passwordSecretName")).thenReturn(null);

        var result = factory.create(dataAddress);
        assertTrue(result.failed());
        verifyNoInteractions(vault);
    }
}