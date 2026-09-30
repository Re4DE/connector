package de.fraunhofer.iee.connector.controlplane.scopes;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.iam.identitytrust.spi.scope.ScopeExtractorRegistry;
import org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestContractNegotiationPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestTransferProcessPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestVersionPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class ScopeExtensionTest {
    private final TypeManager typeManager = mock();
    private final PolicyEngine policyEngine = mock();
    private final SignatureSuiteRegistry signatureSuiteRegistry = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();
    private final ScopeExtractorRegistry scopeExtractorRegistry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(TypeManager.class, typeManager);
        context.registerService(PolicyEngine.class, policyEngine);
        context.registerService(SignatureSuiteRegistry.class, signatureSuiteRegistry);
        context.registerService(TypeTransformerRegistry.class, typeTransformerRegistry);
        context.registerService(ScopeExtractorRegistry.class, scopeExtractorRegistry);

        ScopeExtension extension = factory.constructInstance(ScopeExtension.class);
        extension.initialize(context);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void registerPostValidator() {
        assertNotNull(policyEngine);
        verify(policyEngine).registerPostValidator(eq(RequestCatalogPolicyContext.class), argThat(Objects::nonNull));
        verify(policyEngine).registerPostValidator(eq(RequestContractNegotiationPolicyContext.class), argThat(Objects::nonNull));
        verify(policyEngine).registerPostValidator(eq(RequestTransferProcessPolicyContext.class), argThat(Objects::nonNull));
        verify(policyEngine).registerPostValidator(eq(RequestVersionPolicyContext.class), argThat(Objects::nonNull));
    }
}