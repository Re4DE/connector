# Policy Functions

This extension is a collection of energy sector specific policy functions.

## Settings

| Setting                                 | Example Value                | Description                                                                               |
|-----------------------------------------|------------------------------|-------------------------------------------------------------------------------------------|
| edc.policy.pm.url                       | https://pm.50hertz.com       | The URL of the `50Hertz Permission Administrator`                                         |
| edc.policy.pm.token.url                 | https://pm.50hertz.com/token | The URL of the token endpoint to retrieve JWTs for the `50Hertz Permission Administrator` |
| edc.policy.pm.token.client-id           | john-doe                     | The client id to use the token endpoint                                                   |
| edc.policy.pm.token.client-secret-alias | pm-secret                    | The alias name of the client secret in the `HashiCorp Vault` to use the token endpoint    |

## Permission Administrator Function (Experimental)

With this policy function, the `50Hertz Permission Administrator` can be used to check whether the original
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

## Membership Credential Type Function

With this policy function it can be checked whether a requesting participant is part of the regulated market
communication. That will be validated through the Membership Credential type.

### Usage

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "mako-required",
  "policy": {
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "Membership.membershipType",
          "operator": "eq",
          "rightOperand": "mako"
        }
      }
    ],
    "prohibition": [],
    "obligation": []
  }
}
```

## Identity Function

With this policy function the access to negotiations and the catalog can be restricted to one or multiple participants.

###  Usage with a single participant

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "single-participant-allowed",
  "policy": {
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "identity",
          "operator": "eq",
          "rightOperand": "did:web:iee.fraunhofer.de:alice"
        }
      }
    ],
    "prohibition": [],
    "obligation": []
  }
}
```

###  Usage with multiple participant

```json
{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
  },
  "@id": "multiple-participants-allowed",
  "policy": {
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "identity",
          "operator": "isAnyOf",
          "rightOperand": "did:web:iee.fraunhofer.de:alice,did:web:iee.fraunhofer.de:bob"
        }
      }
    ],
    "prohibition": [],
    "obligation": []
  }
}
```