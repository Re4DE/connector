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
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.*;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.token.spi.TokenValidationService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class X509OAuthService implements IdentityService {

    private static final String ACCEPT = "Accept";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final String GRANT_TYPE = "grant_type";
    private static final String CLIENT_CREDENTIALS = "client_credentials";
    private static final String CLIENT_ID = "client_id";
    private static final String RESPONSE_ACCESS_TOKEN_CLAIM = "access_token";
    private static final String RESPONSE_EXPIRES_IN_CLAIM = "expires_in";
    private static final String OAUTH2_TOKEN_CONTEXT = "oauth2";

    private final TypeManager typeManager;
    private final IdentityProviderKeyResolver providerKeyResolver;
    private final TokenValidationService tokenValidationService;
    private final TokenValidationRulesRegistry tokenValidationRuleRegistry;
    private HttpClient httpClient;
    private HttpRequest request;

    public X509OAuthService(TypeManager typeManager, IdentityProviderKeyResolver providerKeyResolver, TokenValidationService tokenValidationService, TokenValidationRulesRegistry tokenValidationRuleRegistry) {
        this.typeManager = typeManager;
        this.providerKeyResolver = providerKeyResolver;
        this.tokenValidationService = tokenValidationService;
        this.tokenValidationRuleRegistry = tokenValidationRuleRegistry;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters tokenParameters) {
        try {
            var response = this.httpClient.send(this.request, HttpResponse.BodyHandlers.ofString());
            var body = this.typeManager.readValue(response.body(), Map.class);

            var tokenBuilder = TokenRepresentation.Builder.newInstance();
            tokenBuilder.token(body.get(RESPONSE_ACCESS_TOKEN_CLAIM).toString());
            tokenBuilder.expiresIn(Long.parseLong(body.get(RESPONSE_EXPIRES_IN_CLAIM).toString()));

            return Result.success(tokenBuilder.build());
        } catch (Exception e) {
            return Result.failure(e.getMessage());
        }
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, VerificationContext verificationContext) {
        return this.tokenValidationService.validate(tokenRepresentation, this.providerKeyResolver, this.tokenValidationRuleRegistry.getRules(OAUTH2_TOKEN_CONTEXT));
    }

    public void initialize(String certificateRaw, String keyRaw, String tokenUrl, String clientId) {
        this.buildSSLContext(certificateRaw, keyRaw);
        this.buildRequest(tokenUrl, clientId);
    }

    private void buildSSLContext(String certificateRaw, String keyRaw) {
        try {
            var sslContext = SSLContext.getInstance("TLS", "BCJSSE");

            // Parse Private Key
            var privateKeyString = keyRaw
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll("\\R", "")
                    .replace("-----END PRIVATE KEY-----", "");
            var decoded64PrivateKey = Base64.getDecoder().decode(privateKeyString);
            var privateKey = KeyFactory.getInstance("EC", "BC").generatePrivate(new PKCS8EncodedKeySpec(decoded64PrivateKey));

            // Parse Certificate
            var certificateString = certificateRaw
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replaceAll("\\R", "")
                    .replace("-----END CERTIFICATE-----", "");
            var decoded64Certificate = Base64.getDecoder().decode(certificateString);
            var certFactory = CertificateFactory.getInstance("X.509");
            var chain = certFactory.generateCertificates(new ByteArrayInputStream(decoded64Certificate));

            char[] pass = {};
            var clientKeyStore = KeyStore.getInstance("JKS");
            clientKeyStore.load(null, null);
            clientKeyStore.setKeyEntry("client", privateKey, pass, chain.toArray(new Certificate[0]));

            var keyManagerFactory = KeyManagerFactory.getInstance("PKIX");
            keyManagerFactory.init(clientKeyStore, pass);

            // Load default Java trust store
            /*var trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            var defaultX509TrustManager = Arrays
                    .stream(trustManagerFactory.getTrustManagers())
                    .filter((e) -> e instanceof X509TrustManager)
                    .map((e) -> (X509TrustManager) e)
                    .findFirst()
                    .orElseThrow(() -> new EdcException("[X509]: Could not load default X509 trust store"));*/

            // TODO: ONLY FOR TEST, REMOVE!!!
            var trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] x509Certificates, String s)  {}

                        @Override
                        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {}
                    }
            };

            sslContext.init(keyManagerFactory.getKeyManagers(), trustAllCerts /*new TrustManager[] { defaultX509TrustManager }*/, null);
            this.httpClient = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

        } catch (Exception e) {
            throw new EdcException(e);
        }
    }

    private void buildRequest(String tokenUrl, String clientId) {
        var params = new HashMap<String, String>();
        params.put(GRANT_TYPE, CLIENT_CREDENTIALS);
        params.put(CLIENT_ID, clientId);
        var formParams = params.entrySet()
                .stream()
                .map((e) -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        this.request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header(CONTENT_TYPE, FORM_URLENCODED)
                .header(ACCEPT, APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(formParams))
                .build();
    }
}
