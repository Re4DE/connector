# Target Node Resolver SSI Extension

This extension integrates the [Connector Registry](https://gitlab.cc-asp.fraunhofer.de/future-energy-lab-testfeld/connector-registry) as a source to fetch a
list of participants in the data space to create a federated catalog.

See [Helm Chart](../../../chart/README.md) for configuration in a connector.

In the SSI context, the used `sts-tokens` are not suitable for external services.
Therefore, this extension uses the oauth2 client to authenticated against the 
`Connector Registry`. Due to this fact, there is a external `Identity Provider`
needed, such as `Keycloak`.

## Settings

The settings of this extension:

| Setting                                        | Example Value                   | Description                                                                      |
|------------------------------------------------|---------------------------------|----------------------------------------------------------------------------------|
| edc.catalog.registry.enabled                   | true                            | Switch to activate or deactivate an external registry for target node resolution | 
| edc.catalog.registry.url                       | https://registry.dataspace.de   | The url of the deployed `Connector Registry`                                     |
| edc.catalog.registry.token.url                 | https://auth.dataspace.de/token | The url of the oauth2 token provider                                             |
| edc.catalog.registry.token.client.id           | my-connector                    | The client id, used to retrieve a oauth2 token                                   |
| edc.catalog.registry.token.client.secret.alias | my-secret-alias                 | The client secret, used to retrieve a oauth2 token                               |