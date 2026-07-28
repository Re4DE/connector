package de.fraunhofer.iee.connector.common.http.mtls;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.jetty.JettyService;

import java.security.Security;
import java.util.Optional;

@Extension(value = "mTLS Data Plane Extension")
public class MtlsJettyExtension implements ServiceExtension {
    @Setting(key = "edc.web.https.mtls.enabled", description = "Flag to enable mTLS", defaultValue = "false")
    private boolean mtlsEnabled;

    @Setting(key = "edc.web.https.mtls.web.context", description = "Name of the web context to apply mTLS", defaultValue = "public")
    private String mtlsWebContextName;

    @Setting(key = "edc.web.https.mtls.ca.alias", description = "Vault alias for the root CA certificate used to create truststore", defaultValue = "root-ca")
    private String rootCaAlias;

    @Setting(key = "edc.web.https.mtls.key.alias", description = "Vault alias for the private key from server used to create keystore", defaultValue = "client-key")
    private String privateKeyAlias;

    @Setting(key = "edc.web.https.mtls.certificate.alias", description = "Vault alias for the public certificate from server used to create keystore", defaultValue = "client-cert")
    private String publicCertificateAlias;

    @Inject
    private JettyService jettyService;

    @Inject
    private Vault vault;

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (mtlsEnabled) {
            // Add bouncy castle as security provider
            System.setProperty("jdk.tls.namedGroups", String.join(",",
                    "brainpoolP256r1tls13",
                    "brainpoolP384r1tls13",
                    "brainpoolP512r1tls13",
                    "brainpoolP256r1",
                    "brainpoolP384r1",
                    "brainpoolP512r1",
                    "secp256r1",
                    "secp384r1"
            ));
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
            Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
            Security.insertProviderAt(new BouncyCastleJsseProvider(), 2);

            var rawRootCa = Optional.ofNullable(this.vault.resolveSecret(this.rootCaAlias))
                    .orElseThrow(() -> new RuntimeException("Root CA not found in vault"));

            var rawServerPrivateKey = Optional.ofNullable(this.vault.resolveSecret(this.privateKeyAlias))
                    .orElseThrow(() -> new RuntimeException("Private key not found in vault"));

            var rawServerCertificate = Optional.ofNullable(this.vault.resolveSecret(this.publicCertificateAlias))
                    .orElseThrow(() -> new RuntimeException("Public certificate not found in vault"));

            var mtlsService = new MtlsJettyService(
                    this.mtlsWebContextName,
                    this.jettyService,
                    rawRootCa,
                    rawServerPrivateKey,
                    rawServerCertificate,
                    context.getMonitor());
            mtlsService.initialize();
        }
    }

    @Override
    public void shutdown() {
        if (mtlsEnabled) {
            // Remove bouncy castle from runtime
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            Security.removeProvider(BouncyCastleJsseProvider.PROVIDER_NAME);
        }
    }
}