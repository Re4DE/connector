# Target Node Resolver OAuth2 Extension

This extension integrates the [Connector Registry](https://gitlab.cc-asp.fraunhofer.de/future-energy-lab-testfeld/connector-registry) as a source to fetch a
list of participants in the data space to create a federated catalog.

See [Helm Chart](../../../chart/README.md) for configuration in a connector.

The extension reuses the chosen `IdentityService`. Therefore, fetching 
data from the `Connector Registry` is only possible after the connector
is onboarded in a data space with an available `Idenitiy Provider`. 
That means, this extension is only usable with the oauth2 `IdentityService`.

## Settings

The settings of this extension:

| Setting                      | Example Value                 | Description                                                                      |
|------------------------------|-------------------------------|----------------------------------------------------------------------------------|
| edc.catalog.registry.enabled | true                          | Switch to activate or deactivate an external registry for target node resolution | 
| edc.catalog.registry.url     | https://registry.dataspace.de | The url of the deployed `Connector Registry`                                     |
