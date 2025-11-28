/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *       sovity GmbH - added issuedAt leeway
 *
 */

package de.fraunhofer.iee.connector.common.iam.oauth2;

import de.fraunhofer.iee.connector.common.iam.oauth2.identity.IdentityProviderKeyResolver;
import de.fraunhofer.iee.connector.common.iam.oauth2.identity.Oauth2ServiceImpl;
import de.fraunhofer.iee.connector.common.iam.oauth2.jwt.X509CertificateDecorator;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.oauth2.spi.Oauth2AssertionDecorator;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.keys.spi.CertificateResolver;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenDecoratorRegistry;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;

import static java.util.Optional.ofNullable;

/**
 * Provides OAuth2 client credentials flow support.
 */
@Provides({ IdentityService.class })
@Extension(value = Oauth2ServiceExtension.NAME)
public class Oauth2ServiceExtension implements ServiceExtension {

    public static final String NAME = "OAuth2 Identity Service";
    public static final String OAUTH2_TOKEN_CONTEXT = "org/eclipse/edc/iam/oauth2";

    @Setting(description = "URL to obtain OAuth2 JSON Web Key Sets", key = "edc.oauth.provider.jwks.url", defaultValue = "http://localhost/empty_jwks_url")
    public static String jwksUrl;
    @Setting(description = "OAuth2 Token URL", key = "edc.oauth.token.url")
    public static String tokenUrl;
    @Setting(description = "OAuth2 client ID", key = "edc.oauth.client.id")
    public static String clientId;
    @Setting(description = "Vault alias for the private key", key = "edc.oauth.private.key.alias")
    public static String privateKeyAlias;
    @Setting(description = "Vault alias for the certificate", key = "edc.oauth.certificate.alias")
    public static String publicCertificateAlias;
    @Setting(description = "outgoing tokens 'aud' claim value, by default it's the connector id", key = "edc.oauth.provider.audience", required = false)
    public static String providerAudience;
    @Setting(description = "Leeway in seconds for validating the not before (nbf) claim in the token.", defaultValue = "10", key = "edc.oauth.validation.nbf.leeway")
    public static int notBeforeValidationLeeway;
    @Setting(description = "Leeway in seconds for validating the issuedAt claim in the token. By default it is 0 seconds.", defaultValue = "0", key = "edc.oauth.validation.issued.at.leeway")
    public static int issuedAtLeeway;
    @Setting(description = "incoming tokens 'aud' claim required value, by default it's the provider audience value", key = "edc.oauth.endpoint.audience", required = false)
    public static String endpointAudience;
    @Setting(description = "Refresh time for the JWKS, in minutes", key = "edc.oauth.provider.jwks.refresh", defaultValue = "5")
    public static int providerJwksRefresh; // in minutes
    @Setting(description = "Token expiration in minutes. By default is 5 minutes", key = "edc.oauth.token.expiration", defaultValue = "5")
    public static Long tokenExpiration;
    @Setting(description = "Enable the connector to request a token with a specific audience as defined in the RFC-8707.", key = "edc.oauth.token.resource.enabled", defaultValue = "false")
    public static boolean tokenResourceEnabled;

    private IdentityProviderKeyResolver providerKeyResolver;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Inject
    private CertificateResolver certificateResolver;

    @Inject
    private Clock clock;

    @Inject
    private Oauth2Client oauth2Client;

    @Inject
    private TypeManager typeManager;

    @Inject
    private TokenValidationRulesRegistry tokenValidationRulesRegistry;

    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private TokenDecoratorRegistry jwtDecoratorRegistry;
    @Inject
    private JwsSignerProvider jwsSignerProvider;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {

        withDefaults(context);
        warnIfNoLeeway(context.getMonitor());

        var certificate = ofNullable(certificateResolver.resolveCertificate(publicCertificateAlias))
                .orElseThrow(() -> new EdcException("Public certificate not found: " + publicCertificateAlias));
        jwtDecoratorRegistry.register(OAUTH2_TOKEN_CONTEXT, Oauth2AssertionDecorator.Builder.newInstance()
                .audience(providerAudience)
                .clientId(clientId)
                .clock(clock)
                .validity(tokenExpiration)
                .build());
        jwtDecoratorRegistry.register(OAUTH2_TOKEN_CONTEXT, new X509CertificateDecorator(certificate));

        providerKeyResolver = identityProviderKeyResolver(context);

        var oauth2Service = createOauth2Service(jwtDecoratorRegistry, providerKeyResolver);
        context.registerService(IdentityService.class, oauth2Service);

        // add oauth2-specific validation rules
        tokenValidationRulesRegistry.addRule(OAUTH2_TOKEN_CONTEXT, new AudienceValidationRule(endpointAudience));
        tokenValidationRulesRegistry.addRule(OAUTH2_TOKEN_CONTEXT, new NotBeforeValidationRule(clock, notBeforeValidationLeeway, false));
        tokenValidationRulesRegistry.addRule(OAUTH2_TOKEN_CONTEXT, new ExpirationIssuedAtValidationRule(clock, issuedAtLeeway, false));
    }

    private void withDefaults(ServiceExtensionContext context) {
        providerAudience = ofNullable(providerAudience).orElseGet(context::getComponentId);
        endpointAudience = ofNullable(endpointAudience).orElse(providerAudience);
    }

    @Override
    public void start() {
        providerKeyResolver.start();
    }

    @Override
    public void shutdown() {
        providerKeyResolver.stop();
    }

    private IdentityProviderKeyResolver identityProviderKeyResolver(ServiceExtensionContext context) {
        return new IdentityProviderKeyResolver(context.getMonitor(), httpClient, typeManager, jwksUrl, providerJwksRefresh);
    }

    @NotNull
    private Oauth2ServiceImpl createOauth2Service(TokenDecoratorRegistry jwtDecoratorRegistry,
                                                  IdentityProviderKeyResolver providerKeyResolver) {
        String privateKeySupplier = privateKeyAlias;

        return new Oauth2ServiceImpl(
                tokenUrl,
                new JwtGenerationService(jwsSignerProvider),
                privateKeySupplier,
                oauth2Client,
                jwtDecoratorRegistry,
                tokenValidationRulesRegistry,
                tokenValidationService,
                providerKeyResolver,
                tokenResourceEnabled
        );
    }

    private void warnIfNoLeeway(Monitor monitor) {
        if (issuedAtLeeway == 0) {
            var message = "No value was configured for '%s'. Consider setting a leeway of 2-5s in production to avoid problems with clock skew.".formatted("edc.oauth.validation.issued.at.leeway");
            monitor.info(message);
        }
    }
}
