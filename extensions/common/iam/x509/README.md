# X509 Identity Service Extension

This extension adds a new `IdentityService` to the connector.
It´s allow authentication on an `oauth2 provider` using 
`X509 client certificates` during `mTLS`. Therefore, the chosen 
`Identity Provider` need to support `mTLS`. 

The extension supports currently `ONLY` key pairs generated with
elliptic curves!

## Settings

The settings of this extension:

| Setting                    | Example Value           | Description                                                 |
|----------------------------|-------------------------|-------------------------------------------------------------|
| edc.x509.token.url         | https://secure.de/token | The URL of the `oauth2` token endpoint                      |
| edc.x509.client.id         | my-client               | The client id of the connector in the `Idendity Provider`   |
| edc.x509.certificate.alias | client-cert             | The key alias of the client certificate in the `vault`      |
| edc.x509.key.alias         | client-key              | The key alias for the client private key in the `vault`     |
| edc.x509.jwks.url          | https://secure.de/jwks  | The URL of the jwks endpoint used to verify incoming tokens |