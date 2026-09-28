package de.fraunhofer.iee.connector.controlplane.policyfunctions.functions;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MarketPartnerRoleEvaluationFunctionTest {

    private static final String VC_ID = "test-vc";
    private static final String MARKET_PARTNER_CONSTRAINT_KEY = "MarketPartnerCredential";
    private static final String VERIFIABLE_CREDENTIAL_TYPE = "VerifiableCredential";
    private static final String ISSUER_DID = "did:web:test";
    private static final String CREDENTIAL_SUBJECT_ID = "did:web:test";
    private static final String MARKET_ROLE = "marketRole";
    private static final String ROLE_NAME = "roleName";
    private static final String ROLE_ABBREVIATION = "roleAbbreviation";
    private final ParticipantAgentPolicyContext context = mock();
    private final ParticipantAgent participantAgent = mock();
    private final Permission rule = mock();
    private final MarketPartnerRoleEvaluationFunction<ParticipantAgentPolicyContext> function = MarketPartnerRoleEvaluationFunction.create();


    @Nested
    class OperatorValidation {
        @Test
        void shouldFail_whenOperatorIsNotEqOrIsAnyOf() {
            var rightValue = "BIKO";

            var result = function.evaluate(Operator.GT, rightValue, rule, context);
            verify(context).reportProblem("Operator expected to be eq or isAnyOf, but got %s".formatted(Operator.GT.getOdrlRepresentation()));
            assertThat(result).isFalse();
        }
    }

    @Nested
    class RightValueValidation {
        @Test
        void shouldFail_whenSingleRoleWithEq() {
            var rightValue = "BIKO,MSB";

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            verify(context).reportProblem("Multiple roles were provided for right value, use operator isAnyOf");
            assertThat(result).isFalse();
        }

        @Test
        void shouldFail_whenSingleRoleWithIsAnyOf() {
            var rightValue = "BIKO";

            var result = function.evaluate(Operator.IS_ANY_OF, rightValue, rule, context);
            verify(context).reportProblem("A single role was provided for right value, use operator eq");
            assertThat(result).isFalse();
        }

    }

    @Nested
    class ContextAndCredentialValidation {
        @Test
        void shouldFail_whenParticipantAgentIsNull() {
            var rightValue = "BIKO";

            when(context.participantAgent()).thenReturn(null);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            verify(context).reportProblem("No ParticipantAgent found on context.");
            assertThat(result).isFalse();
        }

        @Test
        void shouldFail_whenCredentialListCannotBeResolved() {
            var rightValue = "BIKO";
            when(participantAgent.getClaims()).thenReturn(Map.of());
            when(context.participantAgent()).thenReturn(participantAgent);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            verify(context).reportProblem("ParticipantAgent did not contain a 'vc' claim.");
            assertThat(result).isFalse();
        }

        @Test
        void shouldNotMatch_whenNoMarketPartnerCredentialPresent() {
            var rightValue = "BIKO";
            var verifiableCredential = VerifiableCredential.Builder.newInstance()
                    .id(VC_ID)
                    .type(VERIFIABLE_CREDENTIAL_TYPE)
                    .issuer(new Issuer(ISSUER_DID))
                    .issuanceDate(Instant.now().minus(1, ChronoUnit.DAYS))
                    .expirationDate(Instant.now().plus(365, ChronoUnit.DAYS))
                    .credentialSubject(CredentialSubject.Builder.newInstance()
                            .id(CREDENTIAL_SUBJECT_ID)
                            .claim(MARKET_ROLE, Map.of(ROLE_NAME, "Bilanzkreiskoordinator", ROLE_ABBREVIATION, "BIKO"))
                            .build())
                    .build();

            stubCredentials(verifiableCredential);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isFalse();
        }
    }

    @Nested
    class SingleMarketRole {
        @Test
        void shouldMatch_whenRoleNameEqualsRightValue() {
            var rightValue = "Bilanzkreiskoordinator";

            var vc = credentialWith(Map.of(ROLE_NAME, "Bilanzkreiskoordinator", ROLE_ABBREVIATION, "BIKO"));
            stubCredentials(vc);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isTrue();
        }

        @Test
        void shouldMatch_whenRoleNameIsAnyOfRightValue() {
            var rightValue = "Bilanzkreiskoordinator,Messstellenbetreiber";

            var vc = credentialWith(Map.of(ROLE_NAME, "Bilanzkreiskoordinator", ROLE_ABBREVIATION, "BIKO"));
            stubCredentials(vc);

            var result = function.evaluate(Operator.IS_ANY_OF, rightValue, rule, context);
            assertThat(result).isTrue();
        }

        @Test
        void shouldMatch_whenRoleAbbreviationEqualsRightValue() {
            var rightValue = "MSB";

            var vc = credentialWith(Map.of(ROLE_NAME, "Messstellenbetreiber", ROLE_ABBREVIATION, "MSB"));
            stubCredentials(vc);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isTrue();
        }

        @Test
        void shouldMatch_whenRoleAbbreviationIsAnyOfRightValue() {
            var rightValue = "BIKO,MSB";

            var vc = credentialWith(Map.of(ROLE_NAME, "Messstellenbetreiber", ROLE_ABBREVIATION, "MSB"));
            stubCredentials(vc);

            var result = function.evaluate(Operator.IS_ANY_OF, rightValue, rule, context);
            assertThat(result).isTrue();
        }


        @Test
        void shouldNotMatch_whenNeitherNameNorAbbreviationEqualsRightValue() {
            var rightValue = "MSB";

            var vc = credentialWith(Map.of(ROLE_NAME, "Bilanzkreiskoordinator", ROLE_ABBREVIATION, "BIKO"));
            stubCredentials(vc);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isFalse();
        }

        @Test
        void shouldNotMatch_whenNeitherNameNorAbbreviationIsAnyOfRightValue() {
            var rightValue = "Messstellenbetreiber,LF";

            var vc = credentialWith(Map.of(ROLE_NAME, "Bilanzkreiskoordinator", ROLE_ABBREVIATION, "BIKO"));
            stubCredentials(vc);

            var result = function.evaluate(Operator.IS_ANY_OF, rightValue, rule, context);
            assertThat(result).isFalse();
        }

        @Test
        void shouldNotMatch_whenNoRoleNameIsFound() {
            var rightValue = "Bilanzkreiskoordinator";

            var vc = credentialWith(Map.of("NotAValidRoleName", "Bilanzkreiskoordinator", ROLE_ABBREVIATION, "BIKO"));
            stubCredentials(vc);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isFalse();
        }

        @Test
        void shouldNotMatch_whenNoRoleAbbreviationIsFound() {
            var rightValue = "BIKO";

            var vc = credentialWith(Map.of(ROLE_NAME, "Bilanzkreiskoordinator", "NotAValidRoleAbbreviation", "BIKO"));
            stubCredentials(vc);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isFalse();
        }
    }

    @Nested
    class MultipleMarketRoles {
        @Test
        void shouldMatch_whenAnyRoleNameEqualsRightValue() {
            var rightValue = "Bilanzkreiskoordinator";
            var marketRoles = new ArrayList<Map<String, Object>>();
            marketRoles.add(Map.of(ROLE_NAME, "Bilanzkreiskoordinator", ROLE_ABBREVIATION, "TEST"));
            marketRoles.add(Map.of(ROLE_NAME, "Messstellenbetreiber", ROLE_ABBREVIATION, "MSB"));

            var vc = credentialWith(marketRoles);
            stubCredentials(vc);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isTrue();
        }

        @Test
        void shouldMatch_whenAnyRoleNameIsAnyOfRightValue() {
            var rightValue = "Bilanzkreiskoordinator,Messstellenbetreiber";

            var marketRoles = new ArrayList<Map<String, Object>>();
            marketRoles.add(Map.of(ROLE_NAME, "Bilanzkreiskoordinator", ROLE_ABBREVIATION, "TEST"));
            marketRoles.add(Map.of(ROLE_NAME, "Lieferant", ROLE_ABBREVIATION, "LF"));

            var vc = credentialWith(marketRoles);
            stubCredentials(vc);

            var result = function.evaluate(Operator.IS_ANY_OF, rightValue, rule, context);
            assertThat(result).isTrue();
        }

        @Test
        void shouldMatch_whenAnyRoleAbbreviationEqualsRightValue() {
            var rightValue = "BIKO";
            var marketRoles = new ArrayList<Map<String, Object>>();
            marketRoles.add(Map.of(ROLE_NAME, "Lieferant", ROLE_ABBREVIATION, "BIKO"));
            marketRoles.add(Map.of(ROLE_NAME, "Messstellenbetreiber", ROLE_ABBREVIATION, "MSB"));

            var vc = credentialWith(marketRoles);
            stubCredentials(vc);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isTrue();
        }

        @Test
        void shouldMatch_whenAnyRoleAbbreviationIsAnyOfRightValue() {
            var rightValue = "BIKO,MSB";

            var marketRoles = new ArrayList<Map<String, Object>>();
            marketRoles.add(Map.of(ROLE_NAME, "Lieferant", ROLE_ABBREVIATION, "BIKO"));
            marketRoles.add(Map.of(ROLE_NAME, "Lieferant", ROLE_ABBREVIATION, "LF"));

            var vc = credentialWith(marketRoles);
            stubCredentials(vc);

            var result = function.evaluate(Operator.IS_ANY_OF, rightValue, rule, context);
            assertThat(result).isTrue();
        }

        @Test
        void shouldNotMatch_whenNoRoleNameOrAbbreviationEqualsRightValue() {
            var rightValue = "Lieferant";
            var marketRoles = new ArrayList<Map<String, Object>>();
            marketRoles.add(Map.of(ROLE_NAME, "Bilanzkreiskoordinator", ROLE_ABBREVIATION, "BIKO"));
            marketRoles.add(Map.of(ROLE_NAME, "Messstellenbetreiber", ROLE_ABBREVIATION, "MSB"));

            var vc = credentialWith(marketRoles);
            stubCredentials(vc);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isFalse();
        }

        @Test
        void shouldNotMatch_whenAbbreviationIsAbsent() {
            var rightValue = "Bilanzkreiskoordinator";
            var marketRoles = new ArrayList<Map<String, Object>>();
            marketRoles.add(Map.of(ROLE_NAME, "Bilanzkreiskoordinator"));
            marketRoles.add(Map.of(ROLE_NAME, "Messstellenbetreiber", ROLE_ABBREVIATION, "MSB"));

            var vc = credentialWith(marketRoles);
            stubCredentials(vc);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isTrue();
        }
    }

    @Test
    void shouldNotMatch_whenMarketRoleClaimHasUnexpectedType() {
        var rightValue = "BIKO";

        var vc = credentialWith("UnexpectedType");
        stubCredentials(vc);

        var result = function.evaluate(Operator.EQ, rightValue, rule, context);
        assertThat(result).isFalse();
    }

    private VerifiableCredential credentialWith(Object marketRoleClaim) {
        return VerifiableCredential.Builder.newInstance()
                .id(VC_ID)
                .type(MARKET_PARTNER_CONSTRAINT_KEY)
                .type(VERIFIABLE_CREDENTIAL_TYPE)
                .issuer(new Issuer(ISSUER_DID))
                .issuanceDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .expirationDate(Instant.now().plus(365, ChronoUnit.DAYS))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(CREDENTIAL_SUBJECT_ID)
                        .claim(MARKET_ROLE, marketRoleClaim)
                        .build())
                .build();
    }

    private void stubCredentials(VerifiableCredential... vcs) {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(vcs)));
        when(context.participantAgent()).thenReturn(participantAgent);
    }
}