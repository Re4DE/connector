# Participant Self Registration OAuth2 Extension

This extension handles the registration of the connector into the
[Connector Registry](https://gitlab.cc-asp.fraunhofer.de/future-energy-lab-testfeld/connector-registry).
Is register itself on startup.
A self registered connector appears in the federated data space catalog.
This extension uses the oauth2 identity service.

## Settings

| Setting                              | Example Value                 | Description                                                                                                                                                                    |
|--------------------------------------|-------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| edc.participant.registration.enabled | true                          | Switch to activate/deactivate the self registration in the connector registry. If deactivated, this connector will not appear in the data space catalog of other participants! |
| edc.participant.registration.url     | https://registry.dataspace.de | The url of the deployed `Connector Registry`                                                                                                                                   |