package de.fraunhofer.iee.connector.controlplane.scopes.credentials;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class MarketPartnerCredentialScopeExtractorTest {
    private static final MarketPartnerCredentialScopeExtractor extractor = new MarketPartnerCredentialScopeExtractor();

    @Test
    void extractScopes_withValidLeftValue() {
        String leftValue = "MarketPartner.role";
        var scopes = extractor.extractScopes(leftValue, null, null, null);
        assertThat(scopes).containsExactly("org.eclipse.edc.vc.type:MarketPartnerCredential:read");
    }

    @Test
    void extractScopes_withInvalidLeftValue() {
        String leftValue = "OtherConstraint";
        var scopes = extractor.extractScopes(leftValue, null, null, null);
        assertThat(scopes).isEmpty();
    }

    @Test
    void extractScopes_withAllCapsLeftValue() {
        String leftValue = "MARKETPARTNER.ROLE";
        var scopes = extractor.extractScopes(leftValue, null, null, null);
        assertThat(scopes).isEmpty();
    }

    @Test
    void extractScopes_withNullLeftValue() {
        var scopes = extractor.extractScopes(null, null, null, null);
        assertThat(scopes).isEmpty();
    }

    @Test
    void extractScopes_withNoneStringLeftValue() {
        var scopes = extractor.extractScopes(123, null, null, null);
        assertThat(scopes).isEmpty();
    }

    @Test
    void extractScopes_withEmptyStringLeftValue() {
        var scopes = extractor.extractScopes("", null, null, null);
        assertThat(scopes).isEmpty();
    }
}