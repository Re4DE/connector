# Public API V2

The EDs deprecated the Public API v2 with version 0.14.0.
Therefore, the official EDC does not further support the Public API v2 in the Data Plane. 
Nevertheless, we moved the latest code from the upstream EDC to our implementation. 

The evaluation of a given mTLS Certificate through a header is an experimental feature.

## Settings

| Setting                                   | Example Value                                                   | Description                                                                                                                                    |
|-------------------------------------------|-----------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| edc.dataplane.api.public.baseurl          | https://dataplane.my-dataspace.de/api/public/v2                 | (Optional) Base url of the public API endpoint without the trailing slash. This should point to the public endpoint configured                 |
| edc.dataplane.api.public.response.baseurl | https://dataplane.my-dataspace.de/api/public/v2/responseChannel | (Optional) Base url of the response channel endpoint without the trailing slash. A common practice is to use <PUBLIC_ENDPOINT>/responseChannel |

In addition to the extension settings, it is possible to enable evaluation of a given mTLS certificate. 
To do so, the following options need to be configured in the `dataAddress` field during the asset definition.

| Property                  | Example Value | Description                                                               |
|---------------------------|---------------|---------------------------------------------------------------------------|
| pki:validate              | true          | Flag to activate the evaluation of a given mTLS Certificate from a header |
| pki:headerName            | x-pki-cert    | The header name to extract the mTLS Certificate from                      |

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
    "description": "Retrieve test data from a test api to showcase mTLS certificate evaluation.",
    "contenttype": "application/json"
  },
  "dataAddress": {
    "@type": "DataAddress",
    "type": "HttpData",
    "baseUrl": "https://my.awesome.api.de",
    "pki:validate": "true",
    "pki:headerName": "x-pki-cert"
  }
}
``` 