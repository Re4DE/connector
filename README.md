 # Connector as a Service

[![license](https://img.shields.io/github/license/eclipse-edc/Connector?style=flat-square&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)

---

The Connector is based on the [EDC Connector](https://github.com/eclipse-edc/Connector) in the version 0.14.0.
There are two deployment scenarios, the first uses `Keycloak` as identity provider,
and the second uses the `DCP`. In the following, the first scenario is referred as
`Centralized` and the second as `Decentralized`.

We will discuss here the standalone deployment of a Connector, implying that all required services are up and running.

## Versioning

We use semantic versioning and add the Eclipse Dataspace Components (EDC) version as a label to indicate compatibility.
For example, version `1.0.0-edc0.14.0` means that version `1.0.0` of the Connector is compatible with all EDCs of version `0.14.0`.
If possible, we provide backports of fixes that affect older EDC versions as well.
To get the latest build of the Connector, use the version `SNAPSHOT`.

## Decentralized deployment

The decentralized deployment scenario uses `DCP` as the Identity and Trust framework.
This is the preferred deployment scenario.

### Configuration Control Plane

The configuration parameters in the following table are the minimum fields needed to set up a local startup.
Many more configuration parameters are inherent from the EDCs, compare [EDC](https://github.com/eclipse-edc/Connector).

| Name                                                   | Example Value                                                              | Description                                                                                                   |
|--------------------------------------------------------|----------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| edc.participant.id                                     | did:web:localhost%3A10085:tester                                           | The ID of this Connector, represented as DID:WEB                                                              |
| edc.component.id                                       | tester-connector                                                           | The ID of this runtime component                                                                              |
| edc.hostname                                           | localhost                                                                  | Hostname of the Connector                                                                                     |
| edc.iam.issuer.id                                      | did:web:localhost%3A10085:tester                                           | The DID of the Issuer to get Credentials                                                                      |
| edc.iam.did.web.use.https                              | false                                                                      | Switch between https and http for did providing, only `false` for local development                           |
| edc.iam.sts.oauth.token.url                            | http://localhost:10083/api/sts/token                                       | The url of the STS token endpoint                                                                             |
| edc.iam.sts.oauth.client.id                            | did:web:localhost%3A10085:tester                                           | The client id used to authenticate at the STS endpoints                                                       |
| edc.iam.sts.oauth.client.secret.alias                  | tester-sts-client-secret                                                   | The client secret alias used to authenticate at the STS endpoints                                             |
| edc.iam.credential.revocation.mimetype                 | application/json                                                           | Mimetype of the status list request to get the revocation status of a Credential                              |
| edc.policy.monitor.state-machine.iteration-wait-millis | 30000                                                                      | Time between to re-evaluation processes of policies                                                           |
| web.http.path                                          | /api                                                                       | Default api path                                                                                              |
| web.http.port                                          | 9080                                                                       | Default api port                                                                                              |
| web.http.management.path                               | /api/management                                                            | Management api path                                                                                           |
| web.http.management.port                               | 9081                                                                       | Management api port                                                                                           |
| web.http.management.auth.type                          | tokenbased                                                                 | Type of authentication on the management api, tokenbased means `x-api-key`                                    |
| web.http.management.auth.key                           | devpass                                                                    | The api key used to authenticate on the management api                                                        |
| web.http.control.path                                  | /api/control                                                               | Control token api path                                                                                        |
| web.http.control.port                                  | 9082                                                                       | Control token api port                                                                                        |
| web.http.protocol.path                                 | /api/v1/dsp                                                                | Protocol api path                                                                                             |
| web.http.protocol.port                                 | 9083                                                                       | Protocol api port                                                                                             |
| web.http.version.path                                  | /api/version                                                               | Version api path                                                                                              |
| web.http.version.port                                  | 9085                                                                       | Version api port                                                                                              |
| web.http.catalog.path                                  | /api/catalog                                                               | Catalog path                                                                                                  |
| web.http.catalog.port                                  | 9086                                                                       | Catalog port                                                                                                  |
| edc.vault.hashicorp.url                                | http://localhost:8200                                                      | The url of the `HashiCorp Vault`                                                                              |
| edc.vault.hashicorp.token                              | devpass                                                                    | The `HashiCorp Vault` access token                                                                            |
| edc.sql.schema.autocreate                              | true                                                                       | Flag to autogenerate tables in the `PostgreSQL` if not already done                                           |
| edc.datasource.default.user                            | edc                                                                        | Username to authenticate in the database                                                                      |
| edc.datasource.default.password                        | devpass                                                                    | Password to authenticate in the database                                                                      |
| edc.datasource.default.url                             | jdbc:postgresql://localhost:5432/edc                                       | Connection url of the database                                                                                |
| edc.catalog.registry.url                               | http://localhost:3000/api/registry                                         | The url of the deployed `Connector Registry`                                                                  |                    
| edc.catalog.registry.api.key                           | devpass                                                                    | The api key needed to authenticated at the `Connector Registry`                                               |                                        
| edc.catalog.cache.execution.period.seconds             | 3600                                                                       | Seconds to wait between two runs of the crawlers                                                              |
| edc.catalog.cache.execution.delay.seconds              | 60                                                                         | Seconds to wait before the first run of the crawlers                                                          |
| edc.catalog.cache.partition.num.crawlers               | 1                                                                          | The number of concurrent crawlers using different threads                                                     |
| edc.registration.connector.name                        | tester                                                                     | The human readable connector name                                                                             |
| edc.registration.registry.url                          | http://localhost:3000/api/registry                                         | The url of the deployed `Connector Registry`                                                                  |
| edc.registration.registry.api.key                      | devpass                                                                    | The api key needed to authenticated at the `Connector Registry`                                               |
| edc.registration.keys.name.overwrite                   | tester                                                                     | Override the suffix name of all keys that will saved in the `HashiCorp Vault` for this participant            |
| edc.registration.ih.identity.url                       | http://localhost:10082/api/identity                                        | The identity url of the Identity Hub of this Connector                                                        |
| edc.registration.ih.credentials.url                    | http://localhost:10081/api/credentials                                     | The credential url of the Identity Hub of this Connector                                                      |
| edc.registration.issuer.did                            | did:web:localhost%3A20085:issuer                                           | The DID of the Data Space Issuer, need to be overwriten if not deployed together with Re4DE Data Space Issuer |
| edc.policy.pm.url                                      | https://api-nprd.traxes.io/prprd/forwatt/v2                                | The base url of the `50Hertz Permission Administrator`                                                        |
| edc.policy.pm.token.url                                | https://acc.signin.energy/am/oauth2/realms/root/realms/difesp/access_token | The base url of the token endpoint to authenticate to the `50Hertz Permission Administrator`                  |
| edc.policy.pm.token.client.id                          | my-client-id                                                               | The client id used to get tokens from the token endpoint                                                      |
| edc.policy.pm.token.client.secret.alias                | pm-secret                                                                  | The alias of the client secret used to get tokens from the token endpoint                                     |

### Configuration Data Plane

The configuration parameters in the following table are the minimum fields needed to set up a local startup.
Many more configuration parameters are inherent from the EDCs, compare [EDC](https://github.com/eclipse-edc/Connector).

| Name                                              | Example Value                                    | Description                                                                               |
|---------------------------------------------------|--------------------------------------------------|-------------------------------------------------------------------------------------------|
| edc.participant.id                                | did:web:localhost%3A10085:tester                 | The ID of this Connector, represented as DID:WEB                                          |
| edc.component.id                                  | tester-connector                                 | The ID of this runtime component                                                          |
| edc.hostname                                      | localhost                                        | Hostname of the Connector                                                                 |
| edc.dpf.selector.url                              | http://localhost:9082/api/control/v1/dataplanes  | The url of the control endpoint of the Control Plane where the Data Plane register itself |
| edc.dataplane.api.public.baseurl                  | http://localhost:7084/api/v2/public              | The url of the public api where data can be fetch from                                    |
| edc.transfer.proxy.token.signer.privatekey.alias  | signer-key                                       | The alias key in the `HashiCorp Vault` of the private key to sign requests                |
| edc.transfer.proxy.token.verifier.publickey.alias | verifier-key                                     | The alias key in the `HashiCorp Vault` of the public key to verify requests               |
| web.http.path                                     | /api                                             | Default api path                                                                          |
| web.http.port                                     | 7080                                             | Default api port                                                                          |
| web.http.control.path                             | /api/control                                     | Control token api path                                                                    |
| web.http.control.port                             | 7082                                             | Control token api port                                                                    |
| web.http.public.path                              | /api/v2/public                                   | Public path                                                                               |
| web.http.public.port                              | 7084                                             | Public port                                                                               |
| edc.vault.hashicorp.url                           | http://localhost:8200                            | The url of the `HashiCorp Vault`                                                          |
| edc.vault.hashicorp.token                         | devpass                                          | The `HashiCorp Vault` access token                                                        |
| edc.sql.schema.autocreate                         | true                                             | Flag to autogenerate tables in the `PostgreSQL` if not already done                       |
| edc.datasource.default.user                       | edc                                              | Username to authenticate in the database                                                  |
| edc.datasource.default.password                   | devpass                                          | Password to authenticate in the database                                                  |
| edc.datasource.default.url                        | jdbc:postgresql://localhost:5432/edc             | Connection url of the database                                                            |

### Requirements

The Connector needs other services that are already deployed in a Data Space.
The following components need to be deployed first:

- [Connector Registry](https://github.com/Re4DE/connector-registry)
- [Data Space Issuer](https://github.com/Re4DE/dataspace-issuer)
- [Identity Hub](https://github.com/Re4DE/identity-hub) (Only needed for local development)

### Production Deployment

For a productive deployment, use the provided [Helm Chart](charts/connector-dcp/README.md).
The readme also describes all configurable values.
The production deployment assumes that you use a Kubernetes cluster or a comparable system.

### 1. Create a overwrite.yaml

We recommend creating a `overwrite.yaml` file to overwrite the default values from the Helm Chart.
The deployment of the registry highly depends on your target infrastructure.
The following example file is an orientation only and needs to be adjusted by you to fit your infrastructure.
See the [Helm Chart](charts/connector-dcp/README.md) documentation to see all values that can be configured.

```yaml
# overwrite.yaml

global:
  participantId: did:web:ih.my-connector.de:myself
  connectorName: myself
  
controlplane:
  ingress:
    host: controlplane.my-connector.de
  edc:
    iam:
      stsUrl: https://ih.my-connector.de/api/sts/token
    trustedIssuer:
      0:
        id: did:web:issuer.my-dataspace.de:issuer
        type: ["*"]
    policy:
      permissionAdministrator:
        clientId: my-policyAdmin-id
    registry:
      url: https://connector-registry.my-dataspace.de/api/registry
      apiKey: my-secret-pass-123!
    selfRegistration:
      ihIdentityUrl: https://ih.my-connector.de/api/identity
      ihCredentialUrl: https://ih.my-connector.de/api/credentials
      issuerDid: did:web:issuer.my-dataspace.de:issuer
        
dataplane:
  ingress:
    host: dataplane.my-connector.de
  edc:
    token:
      signer:
        privateKey: |
          -----BEGIN PRIVATE KEY-----
          MIIJAGRALHR...
          -----END PRIVATE KEY-----
      verifier:
        publicKey: |
          -----BEGIN PUBLIC KEY-----
          MIIAJWGEWAG...
          -----END PUBLIC KEY-----
  
identityhub:
  ingress:
    host: ih.my-connector.de
  edc:
    ih:
      seed:
        superUserKey: c3VwZXItdXNlcg==.my-secret-pass-123!

vault:
  dev:
    devRootToken: my-secret-pass-123!
```

### 2. Deploy with Helm

```bash
$ helm dependency update
$ helm install connector -f overwrite.yaml . --namespace connector --create-namespace
```

### Local development

Follow these step to use the `local-dev` runtime.

#### 1. Start environment with docker compose
```shell
cd runtimes/local-dev/docker-env/src/main/docker
docker compose up -d
```

You need to run this command everytime!

#### 2. Start the connector
From project root execute:
```shell
.\gradlew.bat runtimes:local-dev:controlplane-local:build
.\gradlew.bat runtimes:local-dev:dataplane-local:build
java '-Dedc.fs.config=runtimes/local-dev/controlplane-local/src/main/resources/config.properties' -jar runtimes/local-dev/controlplane-local/build/libs/controlplane.jar
java '-Dedc.fs.config=runtimes/local-dev/dataplane-local/src/main/resources/config.properties' -jar runtimes/local-dev/dataplane-local/build/libs/dataplane.jar
```

The build command is only needed if you have done some changes on the source code.
The build and run commands can also be configured to be used out of you IDE like IntelliJ.

### Know issues

## Centralized deployment

The centralized deployment is not recommended. 
Further description on the deployment of a centralized Data Space can be found in the [MVD](https://github.com/Re4DE/mvd).