# Target Node Resolver Extension

This extension integrates the [Connector Registry](https://github.com/Re4DE/connector-registry) as a source to fetch a
list of participants in the Data Space to create a Federated Catalog.

## Settings

The settings of this extension:

| Setting                      | Example Value                 | Description                                                                                  |
|------------------------------|-------------------------------|----------------------------------------------------------------------------------------------|
| edc.catalog.registry.enabled | true                          | Switch to activate or deactivate an external `Connector Registry` for target node resolution | 
| edc.catalog.registry.url     | https://registry.dataspace.de | The url of the deployed `Connector Registry`                                                 |
| edc.catalog.registry.api.key | secret-key-1234!              | The api key needed to authenticated                                                          |