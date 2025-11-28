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

package de.fraunhofer.iee.connector.controlplane.scopes.core;

import org.eclipse.edc.iam.identitytrust.spi.scope.ScopeExtractor;
import org.eclipse.edc.policy.context.request.spi.RequestPolicyContext;
import org.eclipse.edc.policy.model.Operator;

import java.util.Set;

public class MembershipTypeCredentialScopeExtractor implements ScopeExtractor {
    public static final String MEMBERSHIP_CREDENTIAL_TYPE = "MembershipCredential";
    private static final String MEMBERSHIP_TYPE_CONSTRAINT_PREFIX = "Membership.";
    private static final String CREDENTIAL_TYPE_NAMESPACE = "org.eclipse.edc.vc.type";


    @Override
    public Set<String> extractScopes(Object leftValue, Operator operator, Object rightValue, RequestPolicyContext context) {
        Set<String> scopes = Set.of();
        if ((leftValue instanceof String leftOperand)) {
            if (leftOperand.startsWith(MEMBERSHIP_TYPE_CONSTRAINT_PREFIX)) {
                scopes = Set.of("%s:%s:read".formatted(CREDENTIAL_TYPE_NAMESPACE, MEMBERSHIP_CREDENTIAL_TYPE));
            }
        }
        return scopes;
    }
}
