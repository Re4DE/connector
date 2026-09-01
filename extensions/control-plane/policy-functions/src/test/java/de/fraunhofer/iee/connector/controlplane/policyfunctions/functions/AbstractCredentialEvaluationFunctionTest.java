package de.fraunhofer.iee.connector.controlplane.policyfunctions.functions;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractCredentialEvaluationFunctionTest {

    private static final String VC_CLAIM = "vc";
    private final ParticipantAgent participantAgent = mock();
    private final AbstractCredentialEvaluationFunction function = new AbstractCredentialEvaluationFunction();

    @Test
    void shouldReturnCredentialList_whenParticipantAgentContainsCredentials() {
        var verifiableCredential = VerifiableCredential.Builder.newInstance()
                .id("test-vc")
                .type("MembershipCredential")
                .type("VerifiableCredential")
                .issuer(new Issuer("did:web:test"))
                .issuanceDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .expirationDate(Instant.now().plus(365, ChronoUnit.DAYS))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("did:web:test")
                        .claim("holderIdentifier", "BPNL000000000001")
                        .build())
                .build();

        when(participantAgent.getClaims()).thenReturn(Map.of(VC_CLAIM, List.of(verifiableCredential)));

        var result = function.getCredentialList(participantAgent);
        assertThat(result)
                .isSucceeded()
                .asInstanceOf(list(VerifiableCredential.class))
                .containsExactly(verifiableCredential);
    }

    @Test
    void shouldReturnFailure_whenParticipantAgentDoesNotContainVcClaim() {
        when(participantAgent.getClaims()).thenReturn(Map.of());

        var result = function.getCredentialList(participantAgent);
        assertThat(result)
                .isFailed()
                .detail().contains("ParticipantAgent did not contain a '%s' claim.".formatted(VC_CLAIM));
    }

    @Test
    void shouldReturnFailure_whenParticipantAgentContainsVcClaimWithIncorrectType() {
        when(participantAgent.getClaims()).thenReturn(Map.of(VC_CLAIM, "not-a-list"));

        var result = function.getCredentialList(participantAgent);
        assertThat(result)
                .isFailed()
                .detail().contains("ParticipantAgent contains a '%s' claim, but the type is incorrect.".formatted(VC_CLAIM));
    }

    @Test
    void shouldReturnFailure_whenParticipantAgentContainsVcClaimWithEmptyList() {
        when(participantAgent.getClaims()).thenReturn(Map.of(VC_CLAIM, List.of()));

        var result = function.getCredentialList(participantAgent);
        assertThat(result)
                .isFailed()
                .detail().contains("ParticipantAgent contains a '%s' claim but it did not contain any VerifiableCredentials.".formatted(VC_CLAIM));
    }
}