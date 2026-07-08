import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.Security;

import static org.junit.jupiter.api.Assertions.*;

class MtlsE2ETest {

    private static final String PUBLIC_API_URL = "https://localhost:7084/api/v2/public";
    private static final String PASSWORD = "devpass";
    private static final String KEYSTORE_PATH = "src/test/resources/certs/client-keystore.p12";
    private static final String TRUSTSTORE_PATH = "src/test/resources/certs/client-truststore.p12";
    private static final String KEYSTORE_PATH_2 = "src/test/resources/certs/client-keystore-2.p12";
    private static final String TRUSTSTORE_PATH_2 = "src/test/resources/certs/client-truststore-2.p12";

    @BeforeAll
    static void setupProviders() {
        System.setProperty("jdk.tls.namedGroups",
                "brainpoolP256r1tls13,brainpoolP384r1tls13,brainpoolP512r1tls13," +
                        "brainpoolP256r1,brainpoolP384r1,brainpoolP512r1," +
                        "secp256r1,secp384r1");

        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        Security.insertProviderAt(new BouncyCastleJsseProvider(), 2);
    }

    /**
     * This test verifies that a successful mTLS connection can be established
     * when the client keystore and client truststore are signed by a valid root CA.
     */
    @Test
    void shouldConnectWithBrainpoolMtls() throws Exception {
        var sslContext = createSslContext(KEYSTORE_PATH, TRUSTSTORE_PATH);

        var conn = createConnection(sslContext);
        int responseCode = conn.getResponseCode();
        System.out.println("Response Code: " + responseCode);

        assertTrue(responseCode > 0, "Handshake success");

        assertTrue(responseCode == 401 || responseCode == 403,
                "Expected 401/403 without bearer token, got: " + responseCode);

        var errorStream = conn.getErrorStream();
        if (errorStream != null) {
            var body = new String(errorStream.readAllBytes());
            System.out.println("Response Body: " + body);
        }
    }

    /**
     * Verifies that mTLS + valid bearer token grants access to the public API.
     * Note: Token must be a valid EDR token from a transfer process.
     */
    @Test
    void shouldReturn200WithValidToken() throws Exception {
        var sslContext = createSslContext(KEYSTORE_PATH, TRUSTSTORE_PATH);

        var token = "<your-valid-token>";

        var conn = createConnection(sslContext);
        conn.setRequestProperty("Authorization", token);

        int responseCode = conn.getResponseCode();
        System.out.println("Response Code: " + responseCode);

        assertTrue(responseCode != 401 && responseCode != 403,
                "Should not be unauthorized with valid token, got: " + responseCode);

        assertTrue(responseCode >= 200 && responseCode < 300,
                "Expected 2xx response with valid token, got: " + responseCode);

        String body;
        body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        System.out.println("Response Body: " + body);
    }

    /**
     * Verifies that mTLS + invalid token returns a 500 error from the public API.
     *
     */
    @Test
    void shouldReturn500WithInvalidToken() throws Exception {
        var sslContext = createSslContext(KEYSTORE_PATH, TRUSTSTORE_PATH);

        var token = "<your-invalid-token>";

        var conn = createConnection(sslContext);
        conn.setRequestProperty("Authorization", token);

        int responseCode = conn.getResponseCode();
        System.out.println("Response Code: " + responseCode);

        assertEquals(500, responseCode, "Expected 500 response with invalid token, got: " + responseCode);

        String body;
        var errorStream = conn.getErrorStream();
        body = (errorStream != null)
                ? new String(errorStream.readAllBytes(), StandardCharsets.UTF_8)
                : "<no body>";

        System.out.println("Response Body: " + body);
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

        assertTrue(
                exception.getMessage().contains("certificate_unknown")
                        || exception.getCause() instanceof java.security.cert.CertificateException,
                "Some other error: " + exception.getMessage()
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
        System.out.println("Exception: " + exception.getMessage());
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
        var url = URI.create(PUBLIC_API_URL).toURL();
        var conn = (HttpsURLConnection) url.openConnection();
        conn.setSSLSocketFactory(sslContext.getSocketFactory());
        conn.setHostnameVerifier((hostname, session) ->
                "localhost".equals(hostname) || "127.0.0.1".equals(hostname));
        conn.setRequestMethod("GET");
        return conn;
    }
}