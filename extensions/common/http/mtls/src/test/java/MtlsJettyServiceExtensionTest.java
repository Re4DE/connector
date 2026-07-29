import de.fraunhofer.iee.connector.common.http.mtls.MtlsJettyService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.web.jetty.JettyService;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.net.SocketException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.Security;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MtlsJettyServiceExtensionTest {

    private static final String MTLS_WEB_CONTEXT_NAME = "mtls-connector";
    private static final String PASSWORD = "devpass";

    private static final String CERTS_TRUSTED = "src/test/resources/certs/trusted/";
    private static final String CERTS_UNTRUSTED = "src/test/resources/certs/untrusted/";

    private static final String KEYSTORE_TRUSTED_PATH = CERTS_TRUSTED + "client-keystore.p12";
    private static final String TRUSTSTORE_TRUSTED_PATH = CERTS_TRUSTED + "client-truststore.p12";

    private static final String KEYSTORE_UNTRUSTED_PATH = CERTS_UNTRUSTED + "client-keystore.p12";
    private static final String TRUSTSTORE_UNTRUSTED_PATH = CERTS_UNTRUSTED + "client-truststore.p12";

    private static BouncyCastleProvider bcProvider;
    private static BouncyCastleJsseProvider bcJsseProvider;
    private static String previousNamedGroups;
    private static Level previousBcLogLevel;

    private static Server server;
    private static int port;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void setup() throws Exception {
        previousNamedGroups = System.setProperty("jdk.tls.namedGroups",
                "brainpoolP256r1tls13,brainpoolP384r1tls13,brainpoolP512r1tls13," +
                        "brainpoolP256r1,brainpoolP384r1,brainpoolP512r1," +
                        "secp256r1,secp384r1");
        bcProvider = new BouncyCastleProvider();
        bcJsseProvider = new BouncyCastleJsseProvider();
        Security.insertProviderAt(bcProvider, 1);
        Security.insertProviderAt(bcJsseProvider, 2);

        var bcLogger = Logger.getLogger("org.bouncycastle");
        previousBcLogLevel = bcLogger.getLevel();
        bcLogger.setLevel(Level.SEVERE);

        var rootCa = Files.readString(Path.of(CERTS_TRUSTED + "root-ca.crt"));
        var serverKey = Files.readString(Path.of(CERTS_TRUSTED + "server.key"));
        var serverCert = Files.readString(Path.of(CERTS_TRUSTED + "server.crt"));

        var jettyService = mock(JettyService.class);
        var service = new MtlsJettyService(MTLS_WEB_CONTEXT_NAME, jettyService, rootCa, serverKey, serverCert, mock(Monitor.class));
        service.initialize();

        ArgumentCaptor<Consumer<ServerConnector>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(jettyService).addConnectorConfigurationCallback(captor.capture());
        Consumer<ServerConnector> callback = captor.getValue();

        server = new Server();
        var connector = new ServerConnector(server);
        connector.setName(MTLS_WEB_CONTEXT_NAME);
        connector.setPort(0);
        callback.accept(connector);

        server.addConnector(connector);
        server.setHandler(new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                response.setStatus(401);
                callback.succeeded();
                return true;
            }
        });
        server.start();
        port = connector.getLocalPort();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (server != null) {
            server.stop();
        }
        if (previousNamedGroups != null) {
            System.setProperty("jdk.tls.namedGroups", previousNamedGroups);
        }
        Security.removeProvider(bcProvider.getName());
        Security.removeProvider(bcJsseProvider.getName());
        Logger.getLogger("org.bouncycastle").setLevel(previousBcLogLevel);
    }

    /**
     * This test verifies that a successful mTLS connection can be established
     * when the client keystore and client truststore are signed by a valid root CA.
     */
    @Test
    void shouldConnectWithValidMtls() throws Exception {
        var sslContext = createSslContext(KEYSTORE_TRUSTED_PATH, TRUSTSTORE_TRUSTED_PATH);

        var conn = createConnection(sslContext);
        int responseCode = conn.getResponseCode();
        assertTrue(responseCode > 0, "Handshake success");
        assertEquals(401, responseCode, "Expected 401 without token");
    }

    /**
     * This test verifies that the connection fails when the client truststore
     * does not contain the CA that signed the server certificate.
     * The client therefore cannot verify the server's identity.
     */
    @Test
    void shouldFailBecauseClientDoesNotTrustServer() throws Exception {
        var sslContext = createSslContext(KEYSTORE_TRUSTED_PATH, TRUSTSTORE_UNTRUSTED_PATH);

        var conn = createConnection(sslContext);
        var exception = assertThrows(Exception.class, conn::getResponseCode);
        var message = exception.getMessage();
        assertTrue(
                exception instanceof SSLException
                        || exception instanceof SocketException
                        || message.contains("bad_certificate")
                        || message.contains("certificate_unknown")
                        || message.contains("handshake")
                        || message.contains("connection reset"),
                "Some other error: " + exception
        );
    }

    /**
     * This test verifies that the connection fails when the client presents
     * a certificate signed by a CA that the server does not trust.
     * The client truststore is correct, so the client trusts the server,
     * but the server rejects the client.
     */
    @Test
    void shouldFailBecauseServerDoesNotTrustClient() throws Exception {
        var sslContext = createSslContext(KEYSTORE_UNTRUSTED_PATH, TRUSTSTORE_TRUSTED_PATH);

        var conn = createConnection(sslContext);
        var exception = assertThrows(Exception.class, conn::getResponseCode);
        var message = exception.getMessage();
        assertTrue(
                exception instanceof SSLException
                        || exception instanceof SocketException
                        || message.contains("bad_certificate")
                        || message.contains("certificate_unknown")
                        || message.contains("handshake")
                        || message.contains("connection reset"),
                "Some other error: " + exception
        );
    }


    private SSLContext createSslContext(String keystorePath, String truststorePath) throws Exception {
        var keyStore = KeyStore.getInstance("PKCS12", "BC");
        try (var fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, PASSWORD.toCharArray());
        }

        var trustStore = KeyStore.getInstance("PKCS12", "BC");
        try (var fis = new FileInputStream(truststorePath)) {
            trustStore.load(fis, PASSWORD.toCharArray());
        }

        var kmf = KeyManagerFactory.getInstance("PKIX", "BCJSSE");
        kmf.init(keyStore, PASSWORD.toCharArray());

        var tmf = TrustManagerFactory.getInstance("PKIX", "BCJSSE");
        tmf.init(trustStore);

        var sslContext = SSLContext.getInstance("TLSv1.3", "BCJSSE");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext;
    }

    private HttpsURLConnection createConnection(SSLContext sslContext) throws Exception {
        var url = URI.create("https://localhost:" + port + "/").toURL();
        var conn = (HttpsURLConnection) url.openConnection();
        conn.setSSLSocketFactory(sslContext.getSocketFactory());
        conn.setHostnameVerifier((hostname, session) ->
                "localhost".equals(hostname) || "127.0.0.1".equals(hostname));
        conn.setRequestMethod("GET");
        return conn;
    }
}