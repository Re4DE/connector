package de.fraunhofer.iee.connector.controlplane.policyfunctions.functions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class MarketPartnerRoleEvaluationFunction<C extends ParticipantAgentPolicyContext> extends AbstractCredentialEvaluationFunction implements AtomicConstraintRuleFunction<Permission, C> {
    public static final String MARKET_PARTNER_CONSTRAINT_KEY = "MarketPartnerCredential";

    private static final String MARKET_ROLE = "marketRole";
    private static final String ROLE_NAME = "roleName";
    private static final String ROLE_ABBREVIATION = "roleAbbreviation";

    private final ObjectMapper mapper;

    private MarketPartnerRoleEvaluationFunction(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public static <C extends ParticipantAgentPolicyContext> MarketPartnerRoleEvaluationFunction<C> create(ObjectMapper mapper) {
        return new MarketPartnerRoleEvaluationFunction<>(mapper);
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
                    var rawJson = credentialSubject.getClaim("", MARKET_ROLE);
                    if (rawJson instanceof String) {
                        try {
                            var marketRole = this.mapper.readTree((String) rawJson);

                            // There is only a single role
                            if (marketRole.isObject()) {
                                var name = marketRole.get(ROLE_NAME).asText();
                                var abbrv = marketRole.get(ROLE_ABBREVIATION).asText();

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
                            // There are multiple roles
                            } else if (marketRole.isArray()) {
                                boolean isRoleName = marketRole.findValuesAsText(ROLE_NAME)
                                        .stream()
                                        .anyMatch(name -> switch (operator) {
                                            case EQ -> Objects.equals(name, roles.get(0));
                                            case IS_ANY_OF -> roles.stream().anyMatch(role -> Objects.equals(name, role));
                                            default -> false;
                                        });
                                boolean isRoleAbbrv = marketRole.findValuesAsText(ROLE_ABBREVIATION)
                                        .stream()
                                        .anyMatch(abbrv -> switch (operator) {
                                            case EQ -> Objects.equals(abbrv, roles.get(0));
                                            case IS_ANY_OF -> roles.stream().anyMatch(role -> Objects.equals(abbrv, role));
                                            default -> false;
                                        });
                                return isRoleName || isRoleAbbrv;
                            }
                        } catch (Exception e) {
                            context.reportProblem("Could not parse MarketPartnerCredential from participant context");
                        }
                    }
                    return false;
                });
    }
}
