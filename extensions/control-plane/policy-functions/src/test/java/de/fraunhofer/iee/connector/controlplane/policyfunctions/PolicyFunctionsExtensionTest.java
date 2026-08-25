package de.fraunhofer.iee.connector.controlplane.policyfunctions;


import de.fraunhofer.iee.connector.controlplane.policyfunctions.functions.*;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext.CATALOG_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext.NEGOTIATION_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext.TRANSFER_SCOPE;
import static org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext.POLICY_MONITOR_SCOPE;
import static org.mockito.Mockito.*;

@ExtendWith(DependencyInjectionExtension.class)
class PolicyFunctionsExtensionTest {

    private static final String USE = "use";
    private static final String MEMBERSHIP_KEY = "MembershipCredential";
    private static final String IDENTITY_KEY = "identity";
    private static final String PM_KEY = "permission_request_id";
    private static final String ROLE_KEY = "MarketPartner.role";
    private static final String MP_ID_KEY = "MarketPartner.mpId";

    private final RuleBindingRegistry ruleBindingRegistry = mock();
    private final PolicyEngine policyEngine = mock();
    private final EdcHttpClient httpClient = mock();
    private final TypeManager typeManager = mock();
    private final Oauth2Client oauth2Client = mock();
    private final Vault vault = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(RuleBindingRegistry.class, ruleBindingRegistry);
        context.registerService(PolicyEngine.class, policyEngine);
        context.registerService(EdcHttpClient.class, httpClient);
        context.registerService(TypeManager.class, typeManager);
        context.registerService(Oauth2Client.class, oauth2Client);
        context.registerService(Vault.class, vault);

        PolicyFunctionsExtension extension = factory.constructInstance(PolicyFunctionsExtension.class);
        extension.initialize(context);
    }

    @Test
    void bindsUseActionInAllScope() {
        verify(ruleBindingRegistry, times(4)).bind(USE, CATALOG_SCOPE);
    }

    @Test
    void bindMembershipConstraints() {
        verify(ruleBindingRegistry).bind(MEMBERSHIP_KEY, CATALOG_SCOPE);
        verify(ruleBindingRegistry).bind(MEMBERSHIP_KEY, NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry).bind(MEMBERSHIP_KEY, TRANSFER_SCOPE);
    }

    @Test
    void registerMembershipFunction() {
        verify(policyEngine).registerFunction(eq(CatalogPolicyContext.class), eq(Permission.class), eq(MEMBERSHIP_KEY),
                argThat(func -> func instanceof MembershipCredentialEvaluationFunction));

        verify(policyEngine).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Permission.class), eq(MEMBERSHIP_KEY),
                argThat(func -> func instanceof MembershipCredentialEvaluationFunction));

        verify(policyEngine).registerFunction(eq(TransferProcessPolicyContext.class), eq(Permission.class), eq(MEMBERSHIP_KEY),
                argThat(func -> func instanceof MembershipCredentialEvaluationFunction));
    }

    @Test
    void bindMarketPartnerConstraints() {
        verify(ruleBindingRegistry).bind(ROLE_KEY, CATALOG_SCOPE);
        verify(ruleBindingRegistry).bind(ROLE_KEY, NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry).bind(ROLE_KEY, TRANSFER_SCOPE);
        verify(ruleBindingRegistry).bind(MP_ID_KEY, CATALOG_SCOPE);
        verify(ruleBindingRegistry).bind(MP_ID_KEY, NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry).bind(MP_ID_KEY, TRANSFER_SCOPE);
    }

    @Test
    void registerMarketPartnerFunction() {
        verify(policyEngine).registerFunction(eq(CatalogPolicyContext.class), eq(Permission.class), eq(ROLE_KEY),
                argThat(func -> func instanceof MarketPartnerRoleEvaluationFunction));
        verify(policyEngine).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Permission.class), eq(ROLE_KEY),
                argThat(func -> func instanceof MarketPartnerRoleEvaluationFunction));
        verify(policyEngine).registerFunction(eq(TransferProcessPolicyContext.class), eq(Permission.class), eq(ROLE_KEY),
                argThat(func -> func instanceof MarketPartnerRoleEvaluationFunction));
        verify(policyEngine).registerFunction(eq(CatalogPolicyContext.class), eq(Permission.class), eq(MP_ID_KEY),
                argThat(func -> func instanceof MarketPartnerIdEvaluationFunction));
        verify(policyEngine).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Permission.class), eq(MP_ID_KEY),
                argThat(func -> func instanceof MarketPartnerIdEvaluationFunction));
        verify(policyEngine).registerFunction(eq(TransferProcessPolicyContext.class), eq(Permission.class), eq(MP_ID_KEY),
                argThat(func -> func instanceof MarketPartnerIdEvaluationFunction));
    }

    @Test
    void bindPermissionConstraints() {
        verify(ruleBindingRegistry).bind(PM_KEY, NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry).bind(PM_KEY, TRANSFER_SCOPE);
        verify(ruleBindingRegistry).bind(PM_KEY, POLICY_MONITOR_SCOPE);
    }

    @Test
    void registerPermissionFunction() {
        verify(policyEngine).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Permission.class), eq(PM_KEY),
                argThat(func -> func instanceof PermissionAdministratorFunction));
        verify(policyEngine).registerFunction(eq(TransferProcessPolicyContext.class), eq(Permission.class), eq(PM_KEY),
                argThat(func -> func instanceof PermissionAdministratorFunction));
        verify(policyEngine).registerFunction(eq(PolicyMonitorContext.class), eq(Permission.class), eq(PM_KEY),
                argThat(func -> func instanceof PermissionAdministratorFunction));

    }

    @Test
    void bindIdentityConstraints() {
        verify(ruleBindingRegistry).bind(IDENTITY_KEY, CATALOG_SCOPE);
        verify(ruleBindingRegistry).bind(IDENTITY_KEY, NEGOTIATION_SCOPE);
        verify(ruleBindingRegistry).bind(IDENTITY_KEY, TRANSFER_SCOPE);
    }

    @Test
    void registerIdentityFunction() {
        verify(policyEngine).registerFunction(eq(CatalogPolicyContext.class), eq(Permission.class), eq(IDENTITY_KEY),
                argThat(func -> func instanceof IdentityFunction));
        verify(policyEngine).registerFunction(eq(ContractNegotiationPolicyContext.class), eq(Permission.class), eq(IDENTITY_KEY),
                argThat(func -> func instanceof IdentityFunction));
        verify(policyEngine).registerFunction(eq(TransferProcessPolicyContext.class), eq(Permission.class), eq(IDENTITY_KEY),
                argThat(func -> func instanceof IdentityFunction));
    }
}