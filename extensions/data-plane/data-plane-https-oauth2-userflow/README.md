# Data Plane Https `oauth2 userflow` Extension

This extension provides an implementation for the `oauth2 userflow`.
Reusing the `edc oauth2 client` to acquire an access token before data are 
published to the data plane. This means the data plane of the `providing` 
connector deals with authentication on the data source.

## Usage in Asset

The following fields need to be set in the `dataAddress` field:

| Property                  | Example Value           | Description                                       |
|---------------------------|-------------------------|---------------------------------------------------|
| oauth2:tokenUrl           | https://secur.de/tokens | The url of the `outh2` token endpoint             |
| oauth2:username           | joedoe                  | The username                                      |
| oauth2:passwordSecretName | secret-alias            | The secret alias in the vault to get the password |

Example Asset definition:

```json
{
  "@context": [
    "https://w3id.org/edc/connector/management/v0.0.1"
  ],
  "@id": "test-asset",
  "@type": "Asset",
  "properties": {
    "name": "test data",
    "description": "Retrieve test data from a test api to showcase oauth2 userflow in the data plane.",
    "contenttype": "application/json"
  },
  "dataAddress": {
    "@type": "DataAddress",
    "type": "HttpData",
    "baseUrl": "https://my.awesome.api.de",
    "oauth2:tokenUrl": "https://secur.de/tokens",
    "oauth2:username": "joedoe",
    "oauth2:passwordSecretName": "secret-alias"
  }
}
```
