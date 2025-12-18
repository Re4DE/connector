# Participant Self Registration Extension

This extension handles the registration of the connector into the [Connector Registry](https://github.com/Re4DE/connector-registry).
A self registered connector appears in the federated Dataspace catalog.
Furthermore, the connector creates a `ParticipantContext` in its [IdentityHub](https://github.com/Re4DE/identity-hub) and
tries to issue a MembershipCredential, if enabled.

## Settings

| Setting                                      | Example Value                      | Description                                                                                                                                                                                    |
|----------------------------------------------|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| edc.registration.registry.enabled            | true                               | Switch to activate/deactivate the self registration in the connector registry. If deactivated, this connector will not appear in the data space catalog of other participants                  |
| edc.registration.participant.context.enabled | true                               | Switch to activate/deactivate the creation of a participant context in the Identity Hub. If deactivated, this connector can not use SSI                                                        |
| edc.registration.membership.issuance.enabled | true                               | Switch to enable self issuance of a Membership Credential. If deactivated, this connector can not communicate with other participants with SSI. Membership Credential need to be issued manual |
| edc.registration.connector.name              | my-connector                       | A human readable name of the connector, if not used the participant id is then used                                                                                                            |
| edc.registration.registry.url                | https://registry.dataspace.de      | The url of the deployed `Connector Registry`                                                                                                                                                   |
| edc.registration.registry.api.key            | secret-key-1234!                   | The api key needed to authenticated at the `Connector Registry`                                                                                                                                |
| edc.registration.keys.name.override          | short-name                         | Override the suffix name of all keys that will saved in the vault for this participant                                                                                                         |
| edc.registration.ih.identity.url             | https://identityhub/api/identity   | The endpoint of the identity hub identity api to perform participant context creation                                                                                                          |
| edc.registration.ih.credentials.url          | https://identityhub/api/credential | The endpoint of the identity hub credential api to perform to issue a MembershipCredential                                                                                                     |
| edc.registration.issuer.did                  | did:web:dataspaceisser:issuer      | The did of the Dataspace issuer                                                                                                                                                                |
