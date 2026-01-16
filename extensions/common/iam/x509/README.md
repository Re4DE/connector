# X509 Identity Service Extension (Experimental)

This extension adds a new `IdentityService` to the Connector.
It´s allow authentication on an `AOuth2 provider` using 
`X509 client certificates` during `mTLS`. Therefore, the chosen 
`Identity Provider` need to support `mTLS`. 

The extension supports currently `ONLY` key pairs generated with
elliptic curves!

## Settings

The settings of this extension:

| Setting                    | Example Value           | Description                                                       |
|----------------------------|-------------------------|-------------------------------------------------------------------|
| edc.x509.token.url         | https://secure.de/token | The URL of the OAuth2 token endpoint                              |
| edc.x509.client.id         | my-client               | The client id of the Connector in the `Idendity Provider`         |
| edc.x509.certificate.alias | client-cert             | The key alias of the client Certificate in the `HashiCorp Vault`  |
| edc.x509.key.alias         | client-key              | The key alias for the client Private Key in the `HashiCorp Vault` |
| edc.x509.jwks.url          | https://secure.de/jwks  | The URL of the JWKS endpoint used to verify incoming tokens       |