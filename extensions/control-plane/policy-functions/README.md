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
  "@context": [
    "https://w3id.org/edc/connector/management/v0.0.1"
  ],
  "@id": "permission-administrator-required",
  "@type": "PolicyDefinition",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "permission_request_id",
          "operator": "eq",
          "rightOperand": "7ae960cb-464e-43fd-bab8-b176825cc0d2"
        }
      }
    ],
    "prohibition": [],
    "obligation": []
  }
}
```

## Membership Credential active membership

With this policy function it can be checked whether a requesting participant has a active membership

### Usage

```json
{
  "@context": [
    "https://w3id.org/edc/connector/management/v0.0.1"
  ],
  "@type": "PolicyDefinition",
  "@id": "require-membership",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "MembershipCredential",
          "operator": "eq",
          "rightOperand": "active"
        }
      }
    ],
    "prohibition": [],
    "obligation": []
  }
}
```

## Market Partner role Function

With this policy function the access to negotiations and the catalog can be restricted to one or multiple market partner roles.

### Usage with a single role

```json
{
  "@context": [
    "https://w3id.org/edc/connector/management/v0.0.1"
  ],
  "@type": "PolicyDefinition",
  "@id": "only-biko-allowed",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "MarketPartner.role",
          "operator": "eq",
          "rightOperand": "BIKO"
        }
      }
    ],
    "prohibition": [],
    "obligation": []
  }
}
```

### Usage with multiple roles

```json
{
  "@context": [
    "https://w3id.org/edc/connector/management/v0.0.1"
  ],
  "@type": "PolicyDefinition",
  "@id": "biko-msb-allowed",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "MarketPartner.role",
          "operator": "isAnyOf",
          "rightOperand": "BIKO,MSB"
        }
      }
    ],
    "prohibition": [],
    "obligation": []
  }
}
```

## Market Partner ID Function

With this policy function the access to negotiations and the catalog can be restricted to one or multiple market partner ids.

### Usage with a single market partner id

```json
{
  "@context": [
    "https://w3id.org/edc/connector/management/v0.0.1"
  ],
  "@type": "PolicyDefinition",
  "@id": "only-mp-with-id-allowed",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "MarketPartner.mpId",
          "operator": "eq",
          "rightOperand": "4045399000008"
        }
      }
    ],
    "prohibition": [],
    "obligation": []
  }
}
```

### Usage with multiple market partner ids

```json
{
  "@context": [
    "https://w3id.org/edc/connector/management/v0.0.1"
  ],
  "@type": "PolicyDefinition",
  "@id": "only-mps-with-ids-allowed",
  "policy": {
    "@type": "Set",
    "permission": [
      {
        "action": "use",
        "constraint": {
          "leftOperand": "MarketPartner.mpId",
          "operator": "isAnyOf",
          "rightOperand": "4242424242425,4242424242426"
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
  "@context": [
    "https://w3id.org/edc/connector/management/v0.0.1"
  ],
  "@type": "PolicyDefinition",
  "@id": "single-participant-allowed",
  "policy": {
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
  "@context": [
    "https://w3id.org/edc/connector/management/v0.0.1"
  ],
  "@type": "PolicyDefinition",
  "@id": "multiple-participants-allowed",
  "policy": {
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