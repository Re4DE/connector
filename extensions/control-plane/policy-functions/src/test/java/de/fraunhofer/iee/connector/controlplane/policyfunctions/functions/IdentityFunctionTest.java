package de.fraunhofer.iee.connector.controlplane.policyfunctions.functions;

import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IdentityFunctionTest {

    private final ParticipantAgentPolicyContext context = mock();
    private final ParticipantAgent participantAgent = mock();
    private final Permission rule = mock();
    private final IdentityFunction<ParticipantAgentPolicyContext> function = new IdentityFunction<>();

    @Test
    void shouldMatch_whenOperatorIsEqAndIdentityMatches() {
        var rightValue = "did:web:test";

        when(context.participantAgent()).thenReturn(participantAgent);
        when(participantAgent.getIdentity()).thenReturn("did:web:test");

        var result = function.evaluate(Operator.EQ, rightValue, rule, context);
        assertThat(result).isTrue();
    }

    @Test
    void shouldMatch_whenOperatorIsIsAnyOfAndIdentityIsInACommaSeparatedList() {
        var rightValue = "did:web:test,did:web:test2";

        when(context.participantAgent()).thenReturn(participantAgent);
        when(participantAgent.getIdentity()).thenReturn("did:web:test");

        var result = function.evaluate(Operator.IS_ANY_OF, rightValue, rule, context);
        assertThat(result).isTrue();
    }

    @Test
    void shouldFail_whenOperatorIsEqAndIdentityIsInAList() {
        var rightValue = "did:web:test,did:web:test2";

        var result = function.evaluate(Operator.EQ, rightValue, rule, context);
        verify(context).reportProblem("Multiple IDs were provided for right value, use operator isAnyOf");
        assertThat(result).isFalse();
    }

    @Test
    void shouldFail_whenOperatorIsIsAnyOfAndIdentityIsNotInACommaSeparatedList() {
        var rightValue = "did:web:test";

        var result = function.evaluate(Operator.IS_ANY_OF, rightValue, rule, context);
        verify(context).reportProblem("A single ID was provided for right value, use operator eq");
        assertThat(result).isFalse();
    }

    @Test
    void shouldFail_whenOperatorIsNotEqOrIsAnyOf() {
        var rightValue = "did:web:test";

        var result = function.evaluate(Operator.GT, rightValue, rule, context);
        verify(context).reportProblem("Operator expected to be eq or isAnyOf, but got %s".formatted(Operator.GT.getOdrlRepresentation()));
        assertThat(result).isFalse();
    }

    @Test
    void shouldFail_whenParticipantAgentIsNull() {
        var rightValue = "did:web:test";

        when(context.participantAgent()).thenReturn(null);

        var result = function.evaluate(Operator.EQ, rightValue, rule, context);
        verify(context).reportProblem("No ParticipantAgent found on context.");
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotMatch_whenIdentityIsNotEqual() {
        var rightValue = "did:web:test";

        when(context.participantAgent()).thenReturn(participantAgent);
        when(participantAgent.getIdentity()).thenReturn("did:web:test2");

        var result = function.evaluate(Operator.EQ, rightValue, rule, context);
        assertThat(result).isFalse();
    }

    // TODO: Maybe a hint is missing on what just happened. The identity may also be null.
    @Test
    void shouldFail_whenIdentityIsNull() {
        var rightValue = "did:web:test";

        when(context.participantAgent()).thenReturn(participantAgent);
        when(participantAgent.getIdentity()).thenReturn(null);

        var result = function.evaluate(Operator.EQ, rightValue, rule, context);
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotMatch_whenRightValueIsEmpty() {
        var rightValue = "";

        when(context.participantAgent()).thenReturn(participantAgent);
        when(participantAgent.getIdentity()).thenReturn("did:web:test");

        var result = function.evaluate(Operator.EQ, rightValue, rule, context);
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotMatch_whenOperatorIsIsAnyOfAndIdentityIsInACommaSeparatedListWithSpaces() {
        var rightValue = "did:web:test, did:web:test2";

        when(context.participantAgent()).thenReturn(participantAgent);
        when(participantAgent.getIdentity()).thenReturn("did:web:test");

        var result = function.evaluate(Operator.IS_ANY_OF, rightValue, rule, context);
        assertThat(result).isTrue();
    }
}