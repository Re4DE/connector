# Participant Self Registration Extension

This extension handles the registration of the Connector into the [Connector Registry](https://github.com/Re4DE/connector-registry).
A self registered Connector appears in the Federated Data Space Catalog.
Furthermore, the Connector creates a `ParticipantContext` in its [IdentityHub](https://github.com/Re4DE/identity-hub) and
tries to issue a Membership Credential, if enabled.

## Settings

| Setting                                      | Example Value                      | Description                                                                                                                                                                                           |
|----------------------------------------------|------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| edc.registration.registry.enabled            | true                               | Switch to activate/deactivate the self registration in the `Connector Registry`. If deactivated, this Connector will not appear in the Data Space Catalog of other participants                       |
| edc.registration.participant.context.enabled | true                               | Switch to activate/deactivate the creation of a participant context in the Identity Hub. If deactivated, this Connector can not use the DCP                                                           |
| edc.registration.membership.issuance.enabled | true                               | Switch to enable self issuance of a Membership Credential. If deactivated, this Connector can not communicate with other participants through the DCP. Membership Credential need to be issued manual |
| edc.registration.connector.name              | my-connector                       | A human readable name of the Connector, if not used the participant id is then used                                                                                                                   |
| edc.registration.registry.url                | https://registry.dataspace.de      | The url of the deployed `Connector Registry`                                                                                                                                                          |
| edc.registration.registry.api.key            | secret-key-1234!                   | The api key needed to authenticated at the `Connector Registry`                                                                                                                                       |
| edc.registration.keys.name.overwrite         | short-name                         | Overwrite the suffix name of all keys that will saved in the `HashiCorp Vault` for this participant                                                                                                   |
| edc.registration.ih.identity.url             | https://identityhub/api/identity   | The endpoint of the Identity Hub identity api to perform participant context creation                                                                                                                 |
| edc.registration.ih.credentials.url          | https://identityhub/api/credential | The endpoint of the Identity Hub credential api to perform to issue a Membership Credential                                                                                                           |
| edc.registration.issuer.did                  | did:web:dataspaceisser:issuer      | The DID of the Dataspace issuer                                                                                                                                                                       |
