# mTLS Service Extension for Dataplane

This extension enhances the `EDC JettyService` with the ability to enable `mTLS` on a configurable connector.
It uses BouncyCastle as the security provider, enabling support for Brainpool elliptic curves (EC) for key pairs and
certificates.

## Settings

The settings of this extension:

| Setting                              | Example Value | Description                                                                                   |
|--------------------------------------|---------------|-----------------------------------------------------------------------------------------------|
| edc.web.https.mtls.enabled           | true          | Set to true so enabale mTLS                                                                   |
| edc.web.https.mtls.web.context       | public        | Name of the web context where mTLS should be active, for example `public` in the `Data Plane` |
| edc.web.https.mtls.key.alias         | server-cert   | The key alias of the server Certificate in the `HashiCorp Vault`                              |
| edc.web.https.mtls.certificate.alias | server-key    | The key alias for the server Private Key in the `HashiCorp Vault`                             |
| edc.web.https.mtls.ca.alias          | root-ca       | The key alias for the root Certificate in the `HashiCorp Vault`                               |

## Testing

Certificates in `test/resources/trusted` and `test/resources/trusted` can be regenerated, if needed.

### Trusted Root CA and signed certificates regeneration

Create Root CA:

```bash
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:brainpoolP512r1 -out root-ca.key
```

```bash
openssl req -new -x509 -key root-ca.key -out root-ca.crt \
  -days 36500 -sha512 \
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
  -CAcreateserial -out server.crt -days 36500 -sha256 \
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
  -CAcreateserial -out client.crt -days 36500 -sha256
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

### Untrusted Root CA and signed certificates

```bash
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:brainpoolP512r1 -out root-ca.key
```

```bash
openssl req -new -x509 -key root-ca.key -out root-ca.crt \
  -days 36500 -sha512 \
  -subj "/CN=EDC Root CA/O=MyOrg/C=DE" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -addext "keyUsage=critical,keyCertSign,cRLSign"
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
  -CAcreateserial -out client.crt -days 36500 -sha256
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