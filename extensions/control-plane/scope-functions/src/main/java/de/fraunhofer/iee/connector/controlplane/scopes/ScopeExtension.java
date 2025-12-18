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

package de.fraunhofer.iee.connector.controlplane.scopes;

import de.fraunhofer.iee.connector.controlplane.scopes.core.DefaultScopeMappingFunction;
import de.fraunhofer.iee.connector.controlplane.scopes.core.MembershipTypeCredentialScopeExtractor;
import org.eclipse.edc.iam.identitytrust.spi.scope.ScopeExtractorRegistry;
import org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.policy.context.request.spi.RequestCatalogPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestContractNegotiationPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestTransferProcessPolicyContext;
import org.eclipse.edc.policy.context.request.spi.RequestVersionPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;

import java.util.Set;

import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = "Scope extension that handles data space specific scopes, like MembershipVC")
public class ScopeExtension implements ServiceExtension {

    @Inject
    TypeManager typeManager;

    @Inject
    PolicyEngine policyEngine;

    @Inject
    SignatureSuiteRegistry signatureSuiteRegistry;

    @Inject
    TrustedIssuerRegistry trustedIssuerRegistry;

    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;

    @Inject
    private ScopeExtractorRegistry scopeExtractorRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {

        // register signature suite
        var suite = new Jws2020SignatureSuite(this.typeManager.getMapper(JSON_LD));
        this.signatureSuiteRegistry.register(VcConstants.JWS_2020_SIGNATURE_SUITE, suite);

        // register default scopes provider
        var contextMappingFunction = new DefaultScopeMappingFunction(Set.of("org.eclipse.edc.vc.type:MembershipCredential:read"));

        this.policyEngine.registerPostValidator(RequestCatalogPolicyContext.class, contextMappingFunction::apply);
        this.policyEngine.registerPostValidator(RequestContractNegotiationPolicyContext.class, contextMappingFunction::apply);
        this.policyEngine.registerPostValidator(RequestTransferProcessPolicyContext.class, contextMappingFunction::apply);
        this.policyEngine.registerPostValidator(RequestVersionPolicyContext.class, contextMappingFunction::apply);

        // register custom scope extractor
        this.scopeExtractorRegistry.registerScopeExtractor(new MembershipTypeCredentialScopeExtractor());

        // register type transformer for json-ld
        this.typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(this.typeManager, JSON_LD));
    }
}
