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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MtlsE2ETest {

    private static final String MTLS_CONNECTOR = "mtls-connector";
    private static final String PASSWORD = "devpass";
    private static final String CERTS = "src/test/resources/certs/";
    private static final String KEYSTORE_PATH = CERTS + "client-keystore.p12";
    private static final String TRUSTSTORE_PATH = CERTS + "client-truststore.p12";
    private static final String KEYSTORE_PATH_2 = CERTS + "client-keystore-2.p12";
    private static final String TRUSTSTORE_PATH_2 = CERTS + "client-truststore-2.p12";

    private static Server server;
    private static int port;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void setup() throws Exception {
        System.setProperty("jdk.tls.namedGroups",
                "brainpoolP256r1tls13,brainpoolP384r1tls13,brainpoolP512r1tls13," +
                        "brainpoolP256r1,brainpoolP384r1,brainpoolP512r1," +
                        "secp256r1,secp384r1");
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        Security.insertProviderAt(new BouncyCastleJsseProvider(), 2);

        var rootCa = Files.readString(Path.of(CERTS + "root-ca.crt"));
        var serverKey = Files.readString(Path.of(CERTS + "server.key"));
        var serverCert = Files.readString(Path.of(CERTS + "server.crt"));

        var jettyService = mock(JettyService.class);
        var service = new MtlsJettyService(MTLS_CONNECTOR, jettyService, rootCa, serverKey, serverCert, mock(Monitor.class));
        service.initialize();

        ArgumentCaptor<Consumer<ServerConnector>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(jettyService).addConnectorConfigurationCallback(captor.capture());
        Consumer<ServerConnector> callback = captor.getValue();

        server = new Server();
        var connector = new ServerConnector(server);
        connector.setName(MTLS_CONNECTOR);
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
    }

    /**
     * This test verifies that a successful mTLS connection can be established
     * when the client keystore and client truststore are signed by a valid root CA.
     */
    @Test
    void shouldConnectWithValidMtls() throws Exception {
        var sslContext = createSslContext(KEYSTORE_PATH, TRUSTSTORE_PATH);

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
        var sslContext = createSslContext(KEYSTORE_PATH, TRUSTSTORE_PATH_2);

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
        var sslContext = createSslContext(KEYSTORE_PATH_2, TRUSTSTORE_PATH);

        var conn = createConnection(sslContext);
        var exception = assertThrows(Exception.class, conn::getResponseCode);
        var message = exception.getMessage();
        System.out.println("Exception: " + message);
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