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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MembershipCredentialEvaluationFunctionTest {
    private static final String VC_ID = "test-vc";
    private static final String MEMBERSHIP_CONSTRAINT_KEY = "MembershipCredential";
    private static final String VERIFIABLE_CREDENTIAL_TYPE = "VerifiableCredential";
    private static final String ISSUER_DID = "did:web:test";
    private static final String CREDENTIAL_SUBJECT_ID = "did:web:test";
    private static final String MARKET_ROLE = "marketRole";
    private static final String ACTIVE = "active";
    private final ParticipantAgentPolicyContext context = mock();
    private final ParticipantAgent participantAgent = mock();
    private final Permission rule = mock();
    private final MembershipCredentialEvaluationFunction<ParticipantAgentPolicyContext> function = MembershipCredentialEvaluationFunction.create();

    @Nested
    class OperatorValidation {
        @Test
        void shouldFail_whenOperatorIsNotEq() {
            var result = function.evaluate(Operator.GT, ACTIVE, rule, context);
            assertThat(result).isFalse();
            verify(context).reportProblem("Cannot evaluate operator %s, only %s is supported".formatted(Operator.GT, Operator.EQ));
        }
    }

    @Nested
    class RightValueValidation {
        @Test
        void shouldFail_whenRightValueIsNotActive() {
            var rightValue = "inactive";

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);

            assertThat(result).isFalse();
            verify(context).reportProblem("Right-value must be equal to '%s', but was '%s'".formatted(ACTIVE, rightValue));
        }

        @Test
        void shouldFail_whenRightValueIsNotString() {
            var rightValue = 123;

            var result = function.evaluate(Operator.EQ, rightValue, rule, context);

            assertThat(result).isFalse();
        }
    }

    @Nested
    class ContextAndCredentialValidation {
        @Test
        void shouldFail_whenParticipantAgentIsNull() {
            when(context.participantAgent()).thenReturn(null);

            var result = function.evaluate(Operator.EQ, ACTIVE, rule, context);

            assertThat(result).isFalse();
            verify(context).reportProblem("No ParticipantAgent found on context.");
        }

        @Test
        void shouldFail_whenCredentialRetrievalFails() {
            when(context.participantAgent()).thenReturn(participantAgent);
            when(participantAgent.getClaims()).thenReturn(Map.of());

            var result = function.evaluate(Operator.EQ, ACTIVE, rule, context);

            assertThat(result).isFalse();
            verify(context).reportProblem("ParticipantAgent did not contain a 'vc' claim.");
        }

        @Test
        void shouldNotMatch_whenNoMembershipCredentialIsPresent() {
            var nonMembershipCredential = VerifiableCredential.Builder.newInstance()
                    .id(VC_ID)
                    .type(VERIFIABLE_CREDENTIAL_TYPE)
                    .issuer(new Issuer(ISSUER_DID))
                    .issuanceDate(Instant.now().minus(1, ChronoUnit.DAYS))
                    .expirationDate(Instant.now().plus(365, ChronoUnit.DAYS))
                    .credentialSubject(CredentialSubject.Builder.newInstance()
                            .id(CREDENTIAL_SUBJECT_ID)
                            .claim(MARKET_ROLE, Map.of("test-key", "test-value"))
                            .build())
                    .build();
            stubCredentials(nonMembershipCredential);

            var result = function.evaluate(Operator.EQ, ACTIVE, rule, context);

            assertThat(result).isFalse();
        }
    }

    @Nested
    class MembershipValidity {
        @Test
        void shouldMatch_whenValidMembershipCredentialIsPresent() {
            var validCredential = membershipCredential(Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(365, ChronoUnit.DAYS));
            stubCredentials(validCredential);

            var result = function.evaluate(Operator.EQ, ACTIVE, rule, context);

            assertThat(result).isTrue();
        }

        @Test
        void shouldNotMatch_whenMembershipCredentialIsExpired() {
            var expiredCredential = membershipCredential(Instant.now().minus(365, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS));
            stubCredentials(expiredCredential);

            var result = function.evaluate(Operator.EQ, ACTIVE, rule, context);

            assertThat(result).isFalse();
        }

        @Test
        void shouldNotMatch_whenMembershipCredentialIsNotYetValid() {
            var notYetValidCredential = membershipCredential(Instant.now().plus(1, ChronoUnit.DAYS), Instant.now().plus(365, ChronoUnit.DAYS));
            stubCredentials(notYetValidCredential);

            var result = function.evaluate(Operator.EQ, ACTIVE, rule, context);

            assertThat(result).isFalse();
        }

        @Test
        void shouldMatch_whenOneOfMultipleMembershipCredentialsIsValid() {
            var validCredential = membershipCredential(Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(365, ChronoUnit.DAYS));
            var expiredCredential = membershipCredential(Instant.now().minus(365, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS));
            stubCredentials(validCredential, expiredCredential);

            var result = function.evaluate(Operator.EQ, ACTIVE, rule, context);

            assertThat(result).isTrue();
        }
    }

    private VerifiableCredential membershipCredential(Instant issuanceDate, Instant plus) {
        return VerifiableCredential.Builder.newInstance()
                .id(VC_ID)
                .type(MEMBERSHIP_CONSTRAINT_KEY)
                .type(VERIFIABLE_CREDENTIAL_TYPE)
                .issuer(new Issuer(ISSUER_DID))
                .issuanceDate(issuanceDate)
                .expirationDate(plus)
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id(CREDENTIAL_SUBJECT_ID)
                        .claim(MARKET_ROLE, Map.of("test-key", "test-value"))
                        .build())
                .build();
    }

    private void stubCredentials(VerifiableCredential... vcs) {
        when(participantAgent.getClaims()).thenReturn(Map.of("vc", List.of(vcs)));
        when(context.participantAgent()).thenReturn(participantAgent);
    }
}