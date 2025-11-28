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

package de.fraunhofer.iee.connector.controlplane.policyfunctions;

import de.fraunhofer.iee.connector.controlplane.policyfunctions.functions.MembershipCredentialEvaluationFunction;
import de.fraunhofer.iee.connector.controlplane.policyfunctions.functions.PermissionAdministratorFunction;
import de.fraunhofer.iee.connector.controlplane.policyfunctions.oauth2.AccessTokenRetriever;
import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import static org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext.CATALOG_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext.NEGOTIATION_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.TransferProcessPolicyContext.TRANSFER_SCOPE;
import static org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext.POLICY_MONITOR_SCOPE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@Extension(value = "Policy functions for the dataspace")
public class PolicyFunctionsExtension implements ServiceExtension {

    @Setting(key = "edc.policy.pm.url", description = "The endpoint of the permission administrator", required = false, warnOnMissingConfig = true)
    private String pmUrl;

    @Setting(key = "edc.policy.pm.token.url", description = "The endpoint of the permission administrator token service", required = false, warnOnMissingConfig = true)
    private String pmTokenUrl;

    @Setting(key = "edc.policy.pm.token.client.id", description = "The client id for the permission administrator token service", required = false, warnOnMissingConfig = true)
    private String pmClientId;

    @Setting(key = "edc.policy.pm.token.client.secret.alias", description = "The client secret alias for the permission administrator token service", required = false, warnOnMissingConfig = true)
    private String pmClientSecretAlias;

    @Inject
    private EdcHttpClient httpclient;

    @Inject
    private TypeManager typeManager;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private Oauth2Client oauth2Client;

    @Inject
    private Vault vault;

    private static final String PERMISSION_ADMINISTRATOR_POLICY_KEY = EDC_NAMESPACE + "permission_request_id";

    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        this.monitor = context.getMonitor();

        this.registerPMPolicy();
        this.registerMembershipTypePolicy();
    }

    private void registerPMPolicy() {
        var tokenRetriever = new AccessTokenRetriever(this.oauth2Client, this.vault, this.pmTokenUrl, this.pmClientId, this.pmClientSecretAlias);

        this.bindPermissionFunction(new PermissionAdministratorFunction<>(monitor, this.httpclient, this.typeManager, tokenRetriever, this.pmUrl), ContractNegotiationPolicyContext.class, NEGOTIATION_SCOPE, PERMISSION_ADMINISTRATOR_POLICY_KEY);
        this.bindPermissionFunction(new PermissionAdministratorFunction<>(monitor, this.httpclient, this.typeManager, tokenRetriever, this.pmUrl), TransferProcessPolicyContext.class, TRANSFER_SCOPE, PERMISSION_ADMINISTRATOR_POLICY_KEY);
        this.bindPermissionFunction(new PermissionAdministratorFunction<>(monitor, this.httpclient, this.typeManager, tokenRetriever, this.pmUrl), PolicyMonitorContext.class, POLICY_MONITOR_SCOPE, PERMISSION_ADMINISTRATOR_POLICY_KEY);
    }

    private void registerMembershipTypePolicy() {
        var membershipTypeKey = "Membership.membershipType";

        this.bindPermissionFunction(MembershipCredentialEvaluationFunction.create(), CatalogPolicyContext.class, CATALOG_SCOPE, membershipTypeKey);
        this.bindPermissionFunction(MembershipCredentialEvaluationFunction.create(), ContractNegotiationPolicyContext.class, NEGOTIATION_SCOPE, membershipTypeKey);
        this.bindPermissionFunction(MembershipCredentialEvaluationFunction.create(), TransferProcessPolicyContext.class, TRANSFER_SCOPE, membershipTypeKey);
    }

    private <C extends PolicyContext> void bindPermissionFunction(AtomicConstraintRuleFunction<Permission, C> function, Class<C> contextClass, String scope, String constraintType) {
        this.ruleBindingRegistry.bind("use", scope);
        this.ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        this.ruleBindingRegistry.bind(constraintType, scope);

        this.policyEngine.registerFunction(contextClass, Permission.class, constraintType, function);
    }
}
