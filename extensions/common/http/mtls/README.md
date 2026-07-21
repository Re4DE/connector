# mTLS Service Extension for Dataplane

This extension enhances the `EDC JettyService` with the ability to enable `mTLS` on a configurable connector.
It uses BouncyCastle as the security provider, enabling support for Brainpool elliptic curves (EC) for key pairs and
certificates.

## Settings

The settings of this extension:

| Setting                              | Example Value | Description                                                       |
|--------------------------------------|---------------|-------------------------------------------------------------------|
| edc.web.https.mtls.enabled           | true          | Set to true so enabale mTLS                                       |
| edc.web.https.mtls.connector.name    | public        | Name of the connector where mTLS should be active                 |
| edc.web.https.mtls.key.alias         | client-cert   | The key alias of the server Certificate in the `HashiCorp Vault`  |
| edc.web.https.mtls.certificate.alias | client-key    | The key alias for the server Private Key in the `HashiCorp Vault` |
| edc.web.https.mtls.ca.alias          | root-ca       | The key alias for the root Certificate in the `HashiCorp Vault`   |

## Testing

- You need to create certificates, key pairs, keystores and truststore
  for the server and the client.
- The created certificates and keys are only used for testing, don't use them in production.
- Place the created client keystore and truststore in the `test/resources/certs/` folder.

#### Trusted Root CA and signed certificates

Create Root CA:

```bash
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:brainpoolP512r1 -out root-ca.key
```

```bash
openssl req -new -x509 -key root-ca.key -out root-ca.crt \
  -days 365 -sha512 \
  -subj "/CN=EDC Root CA/O=MyOrg/C=DE" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -addext "keyUsage=critical,keyCertSign,cRLSign"
```

Create Server Certificate:

```bash
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:brainpoolP256r1 -out server.key
```

```bash
openssl req -new -key server.key -out server.csr -sha256 \
  -subj "/CN=edc-server.local/O=MyOrg/C=DE" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
```

Server certificate signing:

```bash
openssl x509 -req -in server.csr -CA root-ca.crt -CAkey root-ca.key \
  -CAcreateserial -out server.crt -days 365 -sha256 \
  -copy_extensions copyall
```

Create Client Certificate:

```bash
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:brainpoolP256r1 -out client.key
```

```bash
openssl req -new -key client.key -out client.csr -sha256 \
  -subj "/CN=edc-client/O=MyOrg/C=DE"
```

Client certificate signing:

```bash
openssl x509 -req -in client.csr -CA root-ca.crt -CAkey root-ca.key \
  -CAcreateserial -out client.crt -days 365 -sha256
```

Create Client Keystore and truststore:

```bash
openssl pkcs12 -export -in client.crt -inkey client.key -certfile root-ca.crt \
  -name "client-keystore" -out client-keystore.p12 -password pass:devpass
```

```bash
keytool -importcert -alias root-ca -file root-ca.crt \
  -keystore client-truststore.p12 -storetype PKCS12 \
  -storepass devpass -noprompt
```

Verify Commands

```bash
openssl verify -CAfile root-ca.crt server.crt
openssl verify -CAfile root-ca.crt client.crt
```

#### Untrusted Root CA and signed certificates

To test that mTLS correctly rejects untrusted certificates, create a second Root CA and a second client certificate
signed by it. These are used to simulate a client that is not trusted by the server.

```bash
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:brainpoolP512r1 -out root-ca-2.key
```

```bash
openssl req -new -x509 -key root-ca-2.key -out root-ca-2.crt \
  -days 365 -sha512 \
  -subj "/CN=EDC Root CA/O=MyOrg/C=DE" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -addext "keyUsage=critical,keyCertSign,cRLSign"
```

Create Client Certificate:

```bash
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:brainpoolP256r1 -out client-2.key
```

```bash
openssl req -new -key client-2.key -out client-2.csr -sha256 \
  -subj "/CN=edc-client/O=MyOrg/C=DE"
```

Client certificate signing:

```bash
openssl x509 -req -in client-2.csr -CA root-ca-2.crt -CAkey root-ca-2.key \
  -CAcreateserial -out client-2.crt -days 365 -sha256
```

Create Client Keystore and truststore:

```bash
openssl pkcs12 -export -in client-2.crt -inkey client-2.key -certfile root-ca-2.crt \
  -name "client-keystore" -out client-keystore-2.p12 -password pass:devpass
```

```bash
keytool -importcert -alias root-ca-2 -file root-ca-2.crt \
  -keystore client-truststore-2.p12 -storetype PKCS12 \
  -storepass devpass -noprompt
```