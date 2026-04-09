package de.fraunhofer.iee.connector.controlplane.policyfunctions.functions;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class MarketPartnerIdEvaluationFunction <C extends ParticipantAgentPolicyContext> extends AbstractCredentialEvaluationFunction implements AtomicConstraintRuleFunction<Permission, C> {
    public static final String MARKET_PARTNER_CONSTRAINT_KEY = "MarketPartnerCredential";

    private static final String MARKET_ROLE = "marketRole";
    private static final String MP_ID = "mpId";

    private MarketPartnerIdEvaluationFunction() {}

    public static <C extends ParticipantAgentPolicyContext> MarketPartnerIdEvaluationFunction<C> create() {
        return new MarketPartnerIdEvaluationFunction<>();
    }

    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, C context) {
        // Check operator, need to be EQ or IS_ANY_OF
        if (operator != Operator.EQ && operator != Operator.IS_ANY_OF) {
            context.reportProblem("Operator expected to be eq or isAnyOf, but got %s".formatted(operator.getOdrlRepresentation()));
            return false;
        }

        // Extract the right value
        var rawRightValue = rightValue.toString();
        List<String> mpIds;
        // the right value is a list
        if (rawRightValue.contains(",")) {
            mpIds = Stream.of(rawRightValue.split(",")).map(String::trim).toList();
            // the right value is a single identity
        } else {
            mpIds = List.of(rawRightValue);
        }

        // Cross-check right value with given operator
        if (mpIds.size() == 1 && (operator != Operator.EQ)) {
            context.reportProblem("A single market partner id was provided for right value, use operator eq");
            return false;
        }
        if (mpIds.size() > 1 && (operator != Operator.IS_ANY_OF)) {
            context.reportProblem("Multiple market partner ids were provided for right value, use operator isAnyOf");
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
                .filter(vc -> vc.getType().stream().anyMatch(t -> t.endsWith(MARKET_PARTNER_CONSTRAINT_KEY)))
                .flatMap(credential -> credential.getCredentialSubject().stream())
                .anyMatch(credentialSubject -> {
                    var raw = credentialSubject.getClaim("", MARKET_ROLE);

                    // There is only a single role, so the role can be parsed directly to a Map
                    if (raw instanceof Map) {
                        var marketRole = (Map<String, Object>) raw;
                        var mpId = marketRole.get(MP_ID);

                        // Need to be present otherwise reject
                        if (mpId == null) {
                            return false;
                        }

                        // Check the marked partner id is matching
                        return switch (operator) {
                            case EQ -> Objects.equals(mpId, mpIds.get(0));
                            case IS_ANY_OF -> mpIds.stream().anyMatch(m -> Objects.equals(mpId, m));
                            default -> false;
                        };
                    }

                    // There are multiple roles
                    if (raw instanceof ArrayList) {
                        var marketRoles = (ArrayList<Map<String, Object>>) raw;
                        return marketRoles
                                .stream()
                                .map((m) -> m.get(MP_ID).toString()) // The market partner id will be initially parsed as long, so make a String
                                .anyMatch(mpId -> switch (operator) {
                                    case EQ -> Objects.equals(mpId, mpIds.get(0));
                                    case IS_ANY_OF -> mpIds.stream().anyMatch(m -> Objects.equals(mpId, m));
                                    default -> false;
                                });
                    }

                    return false;
                });
    }
}
