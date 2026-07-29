package de.fraunhofer.iee.connector.common.http.mtls;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.jetty.JettyService;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class MtlsJettyService {

    private static final String TLS = "TLS";
    private static final String BCJSSE = "BCJSSE";
    private static final String BC = "BC";
    private static final String EC = "EC";
    private static final String STORETYPE = "PKCS12";
    private static final String PKIX = "PKIX";
    private static final String X509 = "X.509";
    private static final String SERVER = "server";
    private static final String BEGIN_PRIV = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIV = "-----END PRIVATE KEY-----";
    private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private static final String END_CERT = "-----END CERTIFICATE-----";

    private final String mtlsWebContextName;
    private final JettyService jettyService;
    private final String rawRootCa;
    private final String rawServerPrivateKey;
    private final String rawServerCertificate;
    private final Monitor monitor;

    public MtlsJettyService(String mtlsWebContextName, JettyService jettyService, String rawRootCa, String rawServerPrivateKey, String rawServerCertificate, Monitor monitor) {
        this.mtlsWebContextName = mtlsWebContextName;
        this.jettyService = jettyService;
        this.rawRootCa = rawRootCa;
        this.rawServerPrivateKey = rawServerPrivateKey;
        this.rawServerCertificate = rawServerCertificate;
        this.monitor = monitor;
    }

    public void initialize() {
        this.configureJettyConnector();
    }

    private void configureJettyConnector() {
        monitor.info("Initializing mTLS Jetty extension");
        // Register a connector configuration
        jettyService.addConnectorConfigurationCallback(context -> {
            monitor.info("Configuring context '" + context.getName() + "'");
            // Only apply mTLS configuration to the specified connector
            if (mtlsWebContextName.equals(context.getName())) {
                addSslToConnector(context);
            }
        });
    }

    private void addSslToConnector(ServerConnector context) {
        try {
            monitor.info("Adding mTLS SSL to context '" + context.getName()
                    + "' (port " + context.getPort() + ")");

            var privateKeyString = rawServerPrivateKey
                    .replace(BEGIN_PRIV, "")
                    .replaceAll("\\R", "")
                    .replace(END_PRIV, "");
            var decoded64PrivateKey = Base64.getDecoder().decode(privateKeyString);
            var privateKey = KeyFactory.getInstance(EC, BC).generatePrivate(new PKCS8EncodedKeySpec(decoded64PrivateKey));

            var serverCertChain = parseCertificates(rawServerCertificate);
            var rootCaCert = parseCertificates(rawRootCa);

            char[] pass = {};
            var keyStore = KeyStore.getInstance(STORETYPE, BC);
            keyStore.load(null, null);
            keyStore.setKeyEntry(SERVER, privateKey, pass, serverCertChain.toArray(new Certificate[0]));

            var trustStore = KeyStore.getInstance(STORETYPE, BC);
            trustStore.load(null, null);
            int i = 0;
            for (var cert : rootCaCert) {
                trustStore.setCertificateEntry("root-ca-" + i++, cert);
            }

            // Create key manager and trust manager with PKIX as validation standard and BCJSSE as provider to use bouncy castle implementation
            var kmf = KeyManagerFactory.getInstance(PKIX, BCJSSE);
            kmf.init(keyStore, pass);

            var tmf = TrustManagerFactory.getInstance(PKIX, BCJSSE);
            tmf.init(trustStore);

            // Create SSL context that uses BCJSSE provider for bouncy castle TLS support
            var sslContext = SSLContext.getInstance(TLS, BCJSSE);
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            // Create a new SSL factory and add it to the context that comes from jetty
            var sslContextFactoryServer = new SslContextFactory.Server();
            this.setSslContextFactoryServerConfigurations(sslContext, sslContextFactoryServer);

            var httpsConfig = new HttpConfiguration();
            setHttpsConfigurations(httpsConfig, context);

            context.clearConnectionFactories();
            var sslConnectionFactory = new SslConnectionFactory(
                    sslContextFactoryServer, HttpVersion.HTTP_1_1.asString());
            var httpConnectionFactory = new HttpConnectionFactory(httpsConfig);

            context.addConnectionFactory(sslConnectionFactory);
            context.addConnectionFactory(httpConnectionFactory);
            context.setDefaultProtocol(sslConnectionFactory.getProtocol());

            monitor.info("mTLS with Brainpool EC configured on context '"
                    + context.getName() + "' (port " + context.getPort() + ")");

        } catch (Exception e) {
            throw new EdcException("Failed to add mTLS to context '"
                    + context.getName() + "'", e);
        }
    }

    private void setSslContextFactoryServerConfigurations(SSLContext sslContext, SslContextFactory.Server server) {
        server.setSslContext(sslContext);
        server.setNeedClientAuth(true);
        server.setIncludeProtocols("TLSv1.2", "TLSv1.3");
        // Include only cipher suites compatible with ECDSA (required for EC/brainpool certificates)
        server.setIncludeCipherSuites(
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_AES_256_GCM_SHA384",
                "TLS_AES_128_GCM_SHA256",
                "TLS_AES_128_CCM_SHA256"
        );
    }

    private void setHttpsConfigurations(HttpConfiguration httpsConfig, ServerConnector connector) {
        httpsConfig.setSecureScheme("https");
        httpsConfig.setSecurePort(connector.getPort());
        httpsConfig.addCustomizer(new SecureRequestCustomizer());
        httpsConfig.setSendServerVersion(false);
    }

    private List<Certificate> parseCertificates(String pem) throws CertificateException {
        var certificateString = pem
                .replace(BEGIN_CERT, "")
                .replaceAll("\\R", "")
                .replace(END_CERT, "");
        var decoded64Certificate = Base64.getDecoder().decode(certificateString);
        var certFactory = CertificateFactory.getInstance(X509);
        return new ArrayList<>(certFactory.generateCertificates(
                new ByteArrayInputStream(decoded64Certificate)));
    }
}
