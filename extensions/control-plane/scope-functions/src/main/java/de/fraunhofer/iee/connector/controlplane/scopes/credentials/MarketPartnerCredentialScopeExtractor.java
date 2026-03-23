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

package de.fraunhofer.iee.connector.controlplane.scopes.credentials;

import org.eclipse.edc.iam.identitytrust.spi.scope.ScopeExtractor;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.model.Operator;

import java.util.Set;

public class MarketPartnerCredentialScopeExtractor implements ScopeExtractor {
    private static final String CREDENTIAL_TYPE_NAMESPACE = "org.eclipse.edc.vc.type";
    public static final String MARKET_PARTNER_CREDENTIAL_TYPE = "MarketPartnerCredential";
    private static final String MARKET_PARTNER_CONSTRAINT_PREFIX = "MarketPartner.";

    @Override
    public Set<String> extractScopes(Object leftValue, Operator operator, Object rightValue, RequestPolicyContext context) {
        Set<String> scopes = Set.of();
        if (leftValue instanceof String leftOperand) {
            if (leftOperand.startsWith(MARKET_PARTNER_CONSTRAINT_PREFIX)) {
                scopes = Set.of("%s:%s:read".formatted(CREDENTIAL_TYPE_NAMESPACE, MARKET_PARTNER_CREDENTIAL_TYPE));
            }
        }
        return scopes;
    }
}
