package de.fraunhofer.iee.connector.dataplane.http.oauth2;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParamsProvider;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneHttpOauth2UserFlowExtensionTest {

    private final HttpRequestParamsProvider paramsProvider = mock();
    private final Vault vault = mock();
    private final Oauth2Client oauth2Client = mock();

    private ServiceExtensionContext context;
    private DataPlaneHttpOauth2UserFlowExtension extension;

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        this.context = context;
        context.registerService(HttpRequestParamsProvider.class, paramsProvider);
        context.registerService(Vault.class, vault);
        context.registerService(Oauth2Client.class, oauth2Client);

        extension = factory.constructInstance(DataPlaneHttpOauth2UserFlowExtension.class);
    }

    @Test
    void shouldRegisterOAuth2UserFlowDecorator() {
        extension.initialize(context);
        verify(paramsProvider).registerSourceDecorator(isA(Oauth2UserFlowHttpRequestParamsDecorator.class));
    }

    @Test
    void shouldReturnExtensionName() {
        assertEquals(DataPlaneHttpOauth2UserFlowExtension.NAME, extension.name());
    }
}