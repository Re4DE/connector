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

public class MarketPartnerRoleEvaluationFunction<C extends ParticipantAgentPolicyContext> extends AbstractCredentialEvaluationFunction implements AtomicConstraintRuleFunction<Permission, C> {
    public static final String MARKET_PARTNER_CONSTRAINT_KEY = "MarketPartnerCredential";

    private static final String MARKET_ROLE = "marketRole";
    private static final String ROLE_NAME = "roleName";
    private static final String ROLE_ABBREVIATION = "roleAbbreviation";

    private MarketPartnerRoleEvaluationFunction() {}

    public static <C extends ParticipantAgentPolicyContext> MarketPartnerRoleEvaluationFunction<C> create() {
        return new MarketPartnerRoleEvaluationFunction<>();
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
        List<String> roles;
        // the right value is a list
        if (rawRightValue.contains(",")) {
            roles = Stream.of(rawRightValue.split(",")).map(String::trim).toList();
            // the right value is a single identity
        } else {
            roles = List.of(rawRightValue);
        }

        // Cross-check right value with given operator
        if (roles.size() == 1 && (operator != Operator.EQ)) {
            context.reportProblem("A single role was provided for right value, use operator eq");
            return false;
        }
        if (roles.size() > 1 && (operator != Operator.IS_ANY_OF)) {
            context.reportProblem("Multiple roles were provided for right value, use operator isAnyOf");
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
                        var name = marketRole.get(ROLE_NAME);
                        var abbrv = marketRole.get(ROLE_ABBREVIATION);

                        // Both need to be present otherwise reject
                        if (name == null || abbrv == null) {
                            return false;
                        }

                        // Check whether the full role name or the abbreviation is used
                        return switch (operator) {
                            case EQ -> Objects.equals(name, roles.get(0)) || Objects.equals(abbrv, roles.get(0));
                            case IS_ANY_OF -> roles.stream().anyMatch(role -> Objects.equals(name, role) || Objects.equals(abbrv, role));
                            default -> false;
                        };
                    }

                    // There are multiple roles
                    if (raw instanceof ArrayList) {
                        var marketRoles = (ArrayList<Map<String, Object>>) raw;
                        boolean isRoleName = marketRoles
                                .stream()
                                .map((m) -> m.get(ROLE_NAME))
                                .anyMatch(name -> switch (operator) {
                                    case EQ -> Objects.equals(name, roles.get(0));
                                    case IS_ANY_OF -> roles.stream().anyMatch(role -> Objects.equals(name, role));
                                    default -> false;
                                });
                        boolean isRoleAbbrv = marketRoles
                                .stream()
                                .map((m) -> m.get(ROLE_ABBREVIATION))
                                .anyMatch(abbrv -> switch (operator) {
                                    case EQ -> Objects.equals(abbrv, roles.get(0));
                                    case IS_ANY_OF -> roles.stream().anyMatch(role -> Objects.equals(abbrv, role));
                                    default -> false;
                                });
                        return isRoleName || isRoleAbbrv;
                    }

                    return false;
                });
    }
}
