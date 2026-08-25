package de.fraunhofer.iee.connector.dataplane.http.oauth2;

import de.fraunhofer.iee.iam.oauth2.spi.client.Oauth2UserFlowCredentialsRequest;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static de.fraunhofer.iee.iam.oauth2.spi.Oauth2UserFlowDataAddressSchema.PASSWORD_SECRET_NAME;
import static de.fraunhofer.iee.iam.oauth2.spi.Oauth2UserFlowDataAddressSchema.USERNAME;
import static org.eclipse.edc.iam.oauth2.spi.Oauth2DataAddressSchema.TOKEN_URL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Oauth2UserFlowHttpRequestParamsDecoratorTest {

    private final Oauth2UserFlowCredentialFactory requestFactory = mock();
    private final Oauth2Client client = mock();
    private final DataFlowStartMessage dataFlowStartMessage = mock();
    private final TokenRepresentation tokenRepresentation = mock();

    private HttpDataAddress httpDataAddress;
    private HttpRequestParams.Builder builder;

    @BeforeEach
    void setUp() {
        httpDataAddress = HttpDataAddress.Builder.newInstance()
                .baseUrl("https://localhost:8000")
                .property(TOKEN_URL, "https://localhost:8000/token")
                .property(USERNAME, "test-user")
                .property(PASSWORD_SECRET_NAME, "secretName")
                .build();

        builder = HttpRequestParams.Builder.newInstance()
                .baseUrl("https://localhost:8000")
                .method("GET")
                .contentType("application/json");
    }

    @Test
    void shouldAddAuthorizationHeader_whenAddressUsesOauth2UserFlow() {
        var decorator = new Oauth2UserFlowHttpRequestParamsDecorator(requestFactory, client);

        var credentialsRequest = Oauth2UserFlowCredentialsRequest.Builder.newInstance()
                .url("https://localhost:8000/token")
                .grantType("password")
                .username("test-user")
                .password("resolvedPassword")
                .build();

        when(requestFactory.create(httpDataAddress)).thenReturn(Result.success(credentialsRequest));
        when(client.requestToken(credentialsRequest)).thenReturn(Result.success(tokenRepresentation));
        when(tokenRepresentation.getToken()).thenReturn("access-token");

        var params = decorator.decorate(dataFlowStartMessage, httpDataAddress, builder);

        assertSame(builder, params);
        assertEquals("Bearer access-token", params.build().getHeaders().get("Authorization"));

        verify(requestFactory).create(httpDataAddress);
        verify(client).requestToken(credentialsRequest);

    }

    @Test
    void shouldReturnBuilderUnchanged_whenAddressDoesNotUseOauth2UserFlow() {
        var decorator = new Oauth2UserFlowHttpRequestParamsDecorator(requestFactory, client);

        var httpDataAddress = HttpDataAddress.Builder.newInstance()
                .baseUrl("https://localhost:8000")
                .build();

        when(client.requestToken(any())).thenReturn(Result.success(tokenRepresentation));
        when(tokenRepresentation.getToken()).thenReturn("access-token");

        var result = decorator.decorate(dataFlowStartMessage, httpDataAddress, builder);

        assertSame(builder, result);
        assertNotEquals("Bearer access-token", result.build().getHeaders().get("Authorization"));

        verifyNoInteractions(requestFactory);
        verifyNoInteractions(client);
    }

    @Test
    void shouldThrowException_whenCredentialCreationFails() {
        var decorator = new Oauth2UserFlowHttpRequestParamsDecorator(requestFactory, client);

        when(requestFactory.create(httpDataAddress)).thenReturn(Result.failure("Failed to create credentials"));

        var exception = assertThrows(EdcException.class, () -> {
            decorator.decorate(dataFlowStartMessage, httpDataAddress, builder);
        });

        assertEquals("Cannot authenticate through OAuth2 UserFlow Failed to create credentials", exception.getMessage());
        verify(requestFactory).create(httpDataAddress);
        verifyNoInteractions(client);
    }

    @Test
    void shouldThrowException_whenTokenRequestFails() {
        var decorator = new Oauth2UserFlowHttpRequestParamsDecorator(requestFactory, client);

        var credentialsRequest = Oauth2UserFlowCredentialsRequest.Builder.newInstance()
                .url("https://localhost:8000/token")
                .grantType("password")
                .username("test-user")
                .password("resolvedPassword")
                .build();

        when(requestFactory.create(httpDataAddress)).thenReturn(Result.success(credentialsRequest));
        when(client.requestToken(credentialsRequest)).thenReturn(Result.failure("Failed to request token"));

        var exception = assertThrows(EdcException.class, () -> {
            decorator.decorate(dataFlowStartMessage, httpDataAddress, builder);
        });

        assertEquals("Cannot authenticate through OAuth2 UserFlow Failed to request token", exception.getMessage());
        verify(requestFactory).create(httpDataAddress);
        verify(client).requestToken(credentialsRequest);
    }
}