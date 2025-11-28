# Dena Policy Functions

This extension is a collection of the dena dataspace policy functions.

## Settings

| Setting                                 | Example Value                | Description                                                                     |
|-----------------------------------------|------------------------------|---------------------------------------------------------------------------------|
| edc.policy.pm.url                       | https://pm.50hertz.com       | The URL of the permission administrator                                         |
| edc.policy.pm.token.url                 | https://pm.50hertz.com/token | The URL of the token endpoint to retrieve JWTs for the permission administrator |
| edc.policy.pm.token.client-id           | john-doe                     | The client id to use the token endpoint                                         |
| edc.policy.pm.token.client-secret-alias | pm-secret                    | The alias name of the client secret in the vault to use the token endpoint      |

## Permission Administrator Function

With this policy function, the permission administrator from 50Hertz can be used to check whether the original
data producer, such as a household, gives his/her permission to share the data with the requesting participant.

```
This policy function is currently only for demonstration purpose!
```

### Usage

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "permission-administrator-required",
  "policy": {
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "@type": "AtomicContstraint",
          "leftOperand": "permission_request_id",
          "operator": {
            "@id": "odrl:eq"
          },
          "rightOperand": "7ae960cb-464e-43fd-bab8-b176825cc0d2"
        }
      }
    ],
    "prohibition": [],
    "obligation": []
  }
}
```