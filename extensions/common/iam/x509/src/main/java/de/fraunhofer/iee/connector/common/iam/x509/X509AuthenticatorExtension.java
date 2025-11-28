/*
 *  Copyright (c) 2025 Fraunhofer Institute for Energy Economics and Energy System Technology (IEE)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer IEE - initial API and implementation
 *
 */

package de.fraunhofer.iee.connector.common.iam.x509;

import de.fraunhofer.iee.connector.common.iam.x509.jwk.IdentityProviderKeyResolver;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.*;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;

import java.security.Security;
import java.util.Optional;

@Provides({ IdentityService.class })
@Extension(value = "X509 Identity Service")
public class X509AuthenticatorExtension implements ServiceExtension {

    @Setting(key = "edc.x509.certificate.alias", description = "The public certificate that will be used as client certificate")
    private String publicKeyAlias;

    @Setting(key = "edc.x509.key.alias", description = "The private key of the client certificate")
    private String privateKeyAlias;

    @Setting(key = "edc.x509.token.url", description = "Token endpoint")
    private String tokenUrl;

    @Setting(key = "edc.x509.client.id", description = "Client id")
    private String clientId;

    @Setting(key = "edc.x509.jwks.url", description = "JWKS Url to get public keys for token validation")
    private String jwksUrl;

    @Inject
    private TypeManager typeManager;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private Vault vault;

    @Inject
    private TokenValidationService tokenValidationService;

    @Inject
    private TokenValidationRulesRegistry tokenValidationRulesRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        // Add bouncy castle as security provider
        System.setProperty("jdk.tls.namedGroups", "brainpoolP256r1, brainpoolP384r1, brainpoolP512r1, secp256r1, secp384r1");
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleJsseProvider(), 2);

        var certificateRaw = Optional.ofNullable(this.vault.resolveSecret(this.publicKeyAlias))
                .orElseThrow(() -> new EdcException("[X509] Public certificate not found in vault"));
        var keyRaw = Optional.ofNullable(this.vault.resolveSecret(this.privateKeyAlias))
                .orElseThrow(() -> new EdcException("[X509] Private key not found in vault"));

        var providerKeyResolver = new IdentityProviderKeyResolver(context.getMonitor(), this.httpClient, this.typeManager, this.jwksUrl, 5);

        var x509AuthService = new X509OAuthService(this.typeManager, providerKeyResolver, this.tokenValidationService, this.tokenValidationRulesRegistry);
        x509AuthService.initialize(certificateRaw, keyRaw, this.tokenUrl, this.clientId);
        context.registerService(IdentityService.class, x509AuthService);
    }

    @Override
    public void shutdown() {
        // Remove bouncy castle from runtime
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
    }

    @Provider(isDefault = true)
    public AudienceResolver defaultAudienceResolver() {
        return (msg) -> Result.success(msg.getCounterPartyAddress());
    }
}
