 # Connector as a Service

This Connector is based on the [EDC Connector](https://github.com/eclipse-edc/Connector) in the version 0.14.0.
There are two deployment scenarios, the first uses `Keycloak` as identity provider,
and the second uses `SSI` with `DCP`. In the following, the first scenario is referred as
`oauth2` and the second as `ssi`.

## OAuth2 deployment

### Requirements

The easiest way to use the Connector is to use the Umbrella [Helm Chart](https://gitlab.cc-asp.fraunhofer.de/future-energy-lab-testfeld/umbrella-chart/oauth2).
If the chart is not used, then the following components need to be deployed first:

- [Keycloak](https://gitlab.cc-asp.fraunhofer.de/future-energy-lab-testfeld/identity-chart) with a configured client for the connector
- [Connector Registry](https://gitlab.cc-asp.fraunhofer.de/future-energy-lab-testfeld/connector-registry)

Follow the description in the respective components for more details how to deploy them.

### Configuration

The configuration is depending on the chosen Identity Service:

- [OAuth](charts/connector-oauth/README.md)
- [X509](charts/connector-x509/README.md)

### Local development

Follow these step to use the `local-dev` runtime.

#### 1. Start environment with docker compose
```shell
cd runtimes/local-dev/src/main/docker
docker compose up -f docker-compose-oauth.yaml -d
```

You need to run this command everytime!

#### 2. Start the connector
From project root execute:
```shell
.\gradlew.bat runtimes:local-dev:build
java '-Dedc.fs.config=runtimes/local-dev/src/main/resources/config.properties' -jar runtimes/local-dev/build/libs/local-dev.jar
```

The build command is only needed if you have done some changes on the source code.

### Know issues

#### Cannot access path in local dev

Docker is not able to create the subpath in volumes.
Follow this link in your file explorer, for the ssi stack:

```
\\wsl.localhost\docker-desktop\mnt\docker-desktop-disk\data\docker\volumes\dena-connector-local-dev-ssi_vault_data\_data
```

and create the two folder `file` and `unseal` in that folder.

#### Empty Secret in Vault (productive)

In the current setup only the file backend for the vault is usable.
That means after each redeployment of the vault, all extra created
secret will be deleted and need to be added again by hand.

## SSI deployment

### Requirements

The easiest way to use the Connector is to use the Umbrella [Helm Chart](https://gitlab.cc-asp.fraunhofer.de/future-energy-lab-testfeld/umbrella-chart/ssi).
If the chart is not used, then the following components need to be deployed first:

- [Keycloak](https://gitlab.cc-asp.fraunhofer.de/future-energy-lab-testfeld/identity-chart) with a configured client for the connector
- [Connector Registry](https://gitlab.cc-asp.fraunhofer.de/future-energy-lab-testfeld/connector-registry)
- [Data Space Issuer](https://gitlab.cc-asp.fraunhofer.de/future-energy-lab-testfeld/dataspace-issuer)
- [Identity Hub](https://gitlab.cc-asp.fraunhofer.de/future-energy-lab-testfeld/identity-hub)

### Configuration

The configuration is depending on the chosen Identity Service:

- [SSI](charts/connector-dcp/README.md)

### Local development

Follow these step to use the `local-dev` runtime.

#### 1. Start environment with docker compose
```shell
cd runtimes/local-dev/src/main/docker
docker compose up -f docker-compose-ssi.yaml -d
```

You need to run this command everytime!

#### 2. Start the connector
From project root execute:
```shell
.\gradlew.bat runtimes:local-dev:build
java '-Dedc.fs.config=runtimes/local-dev/src/main/resources/config.properties' -jar runtimes/local-dev/build/libs/local-dev.jar
```

The build command is only needed if you have done some changes on the source code.

### Know issues

#### Cannot access path in local dev

Docker is not able to create the subpath in volumes.
Follow this link in your file explorer:

```
\\wsl.localhost\docker-desktop\mnt\docker-desktop-disk\data\docker\volumes\dena-connector-local-dev_vault_data\_data
```

and create the two folder `file` and `unseal` in that folder.

#### Empty Secret in Vault (productive)

In the current setup only the file backend for the vault is usable.
That means after each redeployment of the vault, all extra created
secret will be deleted and need to be added again by hand.
