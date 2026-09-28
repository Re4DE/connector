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

class MarketPartnerIdEvaluationFunctionTest {

    private static final String VC_ID = "test-vc";
    private static final String MARKET_PARTNER_CONSTRAINT_KEY = "MarketPartnerCredential";
    private static final String VERIFIABLE_CREDENTIAL_TYPE = "VerifiableCredential";
    private static final String ISSUER_DID = "did:web:test";
    private static final String CREDENTIAL_SUBJECT_ID = "did:web:test";
    private static final String MARKET_ROLE = "marketRole";
    private static final String MP_ID = "mpId";
    private final ParticipantAgentPolicyContext context = mock();
    private final ParticipantAgent participantAgent = mock();
    private final Permission rule = mock();
    private final MarketPartnerIdEvaluationFunction<ParticipantAgentPolicyContext> function = MarketPartnerIdEvaluationFunction.create();

    @Nested
    class OperatorValidation {

        @Test
        void shouldFail_whenOperatorIsNotEqOrIsAnyOf() {
            var rightValue = "1234";

            var result = function.evaluate(Operator.GT, rightValue, rule, context);
            assertThat(result).isFalse();
            verify(context).reportProblem("Operator expected to be eq or isAnyOf, but got %s".formatted(Operator.GT.getOdrlRepresentation()));
        }

    }

    @Nested
    class RightValueValidation {

        @Test
        void shouldFail_whenMultipleMpIdsWithEQ() {
            var rightValue = "1234,5678";

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isFalse();
            verify(context).reportProblem("Multiple market partner ids were provided for right value, use operator isAnyOf");
        }

        @Test
        void shouldFail_whenSingleMpIdWithIsAnyOf() {
            var rightValue = "1234";

            var result = function.evaluate(Operator.IS_ANY_OF, rightValue, rule, context);
            assertThat(result).isFalse();
            verify(context).reportProblem("A single market partner id was provided for right value, use operator eq");
        }

    }

    @Nested
    class ContextAndCredentialValidation {

        @Test
        void shouldFail_whenParticipantAgentIsNull() {
            var rightValue = "1234";

            when(context.participantAgent()).thenReturn(null);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isFalse();
            verify(context).reportProblem("No ParticipantAgent found on context.");
        }

        @Test
        void shouldFail_whenCredentialListFailed() {
            var rightValue = "1234";
            when(participantAgent.getClaims()).thenReturn(Map.of());
            when(context.participantAgent()).thenReturn(participantAgent);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isFalse();
            verify(context).reportProblem("ParticipantAgent did not contain a 'vc' claim.");
        }

        @Test
        void shouldNotMatch_whenCredentialTypeIsNotMarketPartnerConstraint() {
            var rightValue = "1234";
            var verifiableCredential = VerifiableCredential.Builder.newInstance()
                    .id(VC_ID)
                    .type(VERIFIABLE_CREDENTIAL_TYPE)
                    .issuer(new Issuer(ISSUER_DID))
                    .issuanceDate(Instant.now().minus(1, ChronoUnit.DAYS))
                    .expirationDate(Instant.now().plus(365, ChronoUnit.DAYS))
                    .credentialSubject(CredentialSubject.Builder.newInstance()
                            .id(CREDENTIAL_SUBJECT_ID)
                            .claim(MARKET_ROLE, Map.of(MP_ID, "1234"))
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
        void shouldMatch_whenMpIdEqualsRightValue() {
            var rightValue = "1234";

            var vc = credentialWith(Map.of(MP_ID, "1234"));
            stubCredentials(vc);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isTrue();
        }

        @Test
        void shouldMatch_whenMpIdIsOneOfRightValue() {
            var rightValue = "1234,5678";

            var vc = credentialWith(Map.of(MP_ID, "1234"));
            stubCredentials(vc);

            var result = function.evaluate(Operator.IS_ANY_OF, rightValue, rule, context);
            assertThat(result).isTrue();
        }

        @Test
        void shouldNotMatch_whenMpIdDiffers() {
            var rightValue = "5678";

            var vc = credentialWith(Map.of(MP_ID, "1234"));
            stubCredentials(vc);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isFalse();
        }

        @Test
        void shouldNotMatch_whenMpIdIsInNoneOfRightValues() {
            var rightValue = "1234,5678";

            var vc = credentialWith(Map.of(MP_ID, "9999"));
            stubCredentials(vc);

            var result = function.evaluate(Operator.IS_ANY_OF, rightValue, rule, context);
            assertThat(result).isFalse();
        }

        @Test
        void shouldNotMatch_whenMpIdClaimIsAbsent() {
            var rightValue = "1234";

            var vc = credentialWith(Map.of("NotAValidMpId", "1234"));
            stubCredentials(vc);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isFalse();
        }
    }

    @Nested
    class MultipleMarketRoles {

        @Test
        void shouldMatch_whenAnyRoleMpIdEqualsRightValue() {
            var rightValue = "12345678942";
            var marketRoles = new ArrayList<Map<String, Object>>();
            marketRoles.add(Map.of(MP_ID, 99999999999L));
            marketRoles.add(Map.of(MP_ID, 12345678942L));

            var vc = credentialWith(marketRoles);
            stubCredentials(vc);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isTrue();
        }

        @Test
        void shouldMatch_whenAnyRoleMpIdIsOneOfRightValues() {
            var rightValue = "12345678942,99999999999";

            var marketRoles = new ArrayList<Map<String, Object>>();
            marketRoles.add(Map.of(MP_ID, 99999999999L));
            marketRoles.add(Map.of(MP_ID, 12345678942L));

            var vc = credentialWith(marketRoles);
            stubCredentials(vc);

            var result = function.evaluate(Operator.IS_ANY_OF, rightValue, rule, context);
            assertThat(result).isTrue();
        }

        @Test
        void shouldNotMatch_whenNoRoleMpIdMatches() {
            var rightValue = "12345678942";
            var marketRoles = new ArrayList<Map<String, Object>>();
            marketRoles.add(Map.of(MP_ID, 99999999999L));
            marketRoles.add(Map.of(MP_ID, 88888888888L));

            var vc = credentialWith(marketRoles);
            stubCredentials(vc);

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);
            assertThat(result).isFalse();
        }

        @Test
        void shouldNotMatch_whenNoRoleMpIdIsInRightValues() {
            var rightValue = "12345678942,77777777777";

            var marketRoles = new ArrayList<Map<String, Object>>();
            marketRoles.add(Map.of(MP_ID, 99999999999L));
            marketRoles.add(Map.of(MP_ID, 88888888888L));

            var vc = credentialWith(marketRoles);
            stubCredentials(vc);

            var result = function.evaluate(Operator.IS_ANY_OF, rightValue, rule, context);
            assertThat(result).isFalse();
        }

    }

    @Test
    void shouldNotMatch_whenMarketRoleClaimHasUnexpectedType() {
        var vc = credentialWith("unexpectedType");
        stubCredentials(vc);

        var result = function.evaluate(Operator.EQ, "12345678942", rule, context);
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