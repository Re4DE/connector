# Participant Self Registration SSI Extension

This extension handles the registration of the connector into the
[Connector Registry](https://gitlab.cc-asp.fraunhofer.de/future-energy-lab-testfeld/connector-registry). Is register itself on startup.
A self registered connector appears in the federated data space catalog.
Furthermore, the connector creates a `ParticipantContext` in its [IdentityHub](https://gitlab.cc-asp.fraunhofer.de/future-energy-lab-testfeld/identity-hub) and
tries to issue a MembershipCredential.

## Settings

| Setting                                                  | Example Value                      | Description                                                                                                                                                                           |
|----------------------------------------------------------|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| edc.participant.registration.enabled                     | true                               | Switch to activate/deactivate the self registration in the connector registry. If deactivated, this connector will not appear in the data space catalog of other participants         |
| edc.participant.registration.membership.issuance.enabled | true                               | Switch to enable self issuance of a Membership Credential. If deactivated, this connector can not communicate with other participants. Membership Credential need to be issued manuel |
| edc.participant.registration.url                         | https://registry.dataspace.de      | The url of the deployed `Connector Registry`                                                                                                                                          |
| edc.participant.registration.token.url                   | https://auth.dataspace.de/token    | The url of the oauth2 token provider                                                                                                                                                  |
| edc.participant.registration.token.client.id             | my-connector                       | The client id, used to retrieve a oauth2 token                                                                                                                                        |
| edc.participant.registration.token.client.secret.alias   | my-secret-alias                    | The client secret, used to retrieve a oauth2 token                                                                                                                                    |
| edc.participant.registration.keys.name.override          | short-name                         | Override the suffix name of all keys that will saved in the vault for this participant                                                                                                |
| edc.participant.registration.ih.identity.url             | https://identityhub/api/identity   | The identity url of the identity hub of this connector                                                                                                                                |
| edc.participant.registration.ih.credentials.url          | https://identityhub/api/credential | The credential url of the identity hub of this connector                                                                                                                              |
| edc.participant.registration.issuer.did                  | did:web:dataspaceisser:issuer      | The did of the data space issuer                                                                                                                                                      |
