package de.fraunhofer.iee.connector.controlplane.policyfunctions.functions;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;

import java.util.List;
import java.util.stream.Stream;

public class IdentityFunction<C extends ParticipantAgentPolicyContext> implements AtomicConstraintRuleFunction<Permission, C> {

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, C context) {
        // Check operator, need to be EQ or IS_ANY_OF
        if (operator != Operator.EQ && operator != Operator.IS_ANY_OF) {
            context.reportProblem("Operator expected to be eq or isAnyOf, but got %s".formatted(operator.getOdrlRepresentation()));
            return false;
        }

        // Extract the right value
        var rawRightValue = rightValue.toString();
        List<String> identities;
        // the right value is a list
        if (rawRightValue.contains(",")) {
            identities = Stream.of(rawRightValue.split(",")).map(String::trim).toList();
            // the right value is a single identity
        } else {
            identities = List.of(rawRightValue);
        }

        // Cross-check right value with given operator
        if (identities.size() == 1 && (operator != Operator.EQ)) {
            context.reportProblem("A single ID was provided for right value, use operator eq");
            return false;
        }
        if (identities.size() > 1 && (operator != Operator.IS_ANY_OF)) {
            context.reportProblem("Multiple IDs were provided for right value, use operator isAnyOf");
            return false;
        }

        // Get counterparty ID
        var pa = context.participantAgent();
        if (pa == null) {
            context.reportProblem("No ParticipantAgent found on context.");
            return false;
        }
        var identity = pa.getIdentity();

        return switch (operator) {
            case EQ -> identities.get(0).equals(identity);
            case IS_ANY_OF -> identities.stream().anyMatch((i) -> i.equals(identity));
            default -> false;
        };
    }
}
