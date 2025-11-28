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

package de.fraunhofer.iee.connector.controlplane.policyfunctions.functions;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;

import java.util.Map;

public class MembershipCredentialEvaluationFunction<C extends ParticipantAgentPolicyContext> extends AbstractCredentialEvaluationFunction implements AtomicConstraintRuleFunction<Permission, C> {
    public static final String MEMBERSHIP_CONSTRAINT_KEY = "MembershipCredential";

    private static final String MEMBERSHIP_CLAIM = "membership";
    private static final String TYPE_CLAIM = "membershipType";
    private static final String MAKO = "mako";

    private MembershipCredentialEvaluationFunction() {}

    public static <C extends ParticipantAgentPolicyContext> MembershipCredentialEvaluationFunction<C> create() {
        return new MembershipCredentialEvaluationFunction<C>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, C context) {
        if (!operator.equals(Operator.EQ)) {
            context.reportProblem("Cannot evaluate operator %s, only %s is supported".formatted(operator, Operator.EQ));
            return false;
        }
        if (!MAKO.equals(rightValue)) {
            context.reportProblem("Right-value must be equal to '%s', but was '%s'".formatted(MAKO, rightValue));
            return false;
        }

        var pa = context.participantAgent();
        if (pa == null) {
            context.reportProblem("No ParticipantAgent found on context.");
            return false;
        }

        var credentialResult = getCredentialList(pa);
        if (credentialResult.failed()) {
            context.reportProblem(credentialResult.getFailureDetail());
            return false;
        }

        return credentialResult.getContent()
                .stream()
                .filter(vc -> vc.getType().stream().anyMatch(t -> t.endsWith(MEMBERSHIP_CONSTRAINT_KEY)))
                .flatMap(vc -> vc.getCredentialSubject().stream().filter(cs -> cs.getClaims().containsKey(MEMBERSHIP_CLAIM)))
                .anyMatch(credential -> {
                    var claim = (Map<String, ?>) credential.getClaim("", MEMBERSHIP_CLAIM);
                    var type = claim.get(TYPE_CLAIM);
                    return MAKO.equals(type);
                });
    }
}
