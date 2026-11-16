package de.fraunhofer.iee.connector.controlplane.registration.util;

import jakarta.json.Json;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class RequestBuilder {
    private static final String X_API_KEY = "x-api-key";
    private static final String PROTOCOL_ENDPOINT = "ProtocolEndpoint";
    private static final MediaType TYPE_JSON = MediaType.parse("application/json");

    private static final String ACTIVE = "active";
    private static final String PARTICIPANT_ID = "participantId";
    private static final String PARTICIPANT_CONTEXT_ID = "participantContextId";
    private static final String DID = "did";
    private static final String SCOPES = "scopes";
    private static final String SERVICE_ENDPOINTS = "serviceEndpoints";
    private static final String TYPE = "type";
    private static final String SERVICE_ENDPOINT = "serviceEndpoint";
    private static final String ID = "id";
    private static final String KEY = "key";
    private static final String KEY_ID = "keyId";
    private static final String PRIVATE_KEY_ALIAS = "privateKeyAlias";
    private static final String KEY_GENERATOR_PARAMS = "keyGeneratorParams";
    private static final String ALGORITHM = "algorithm";
    private static final String EC = "EC";
    private static final String ADDITIONAL_PROPERTIES = "additionalProperties";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String ISSUER_DID = "issuerDid";
    private static final String CREDENTIALS = "credentials";
    private static final String FORMAT = "format";
    private static final String MEMBERSHIP_CREDENTIAL_DEF = "membership-credential-def";
    private static final String MEMBERSHIP_CREDENTIAL = "MembershipCredential";
    private static final String CREDENTIAL_FORMAT = "vc20-bssl/jwt";
    private static final String MARKET_PARTNER_CREDENTIAL_DEF = "marketpartner-credential-def";
    private static final String MARKET_PARTNER_CREDENTIAL = "MarketPartnerCredential";

    public static Request buildGetParticipantContextRequest(String identityUrl, String participantContextId, String superUserApiKey) {
        return new Request.Builder()
                .url("%s/v1beta/participants/%s".formatted(identityUrl, participantContextId))
                .addHeader(X_API_KEY, superUserApiKey)
                .build();
    }

    public static Request buildCreateParticipantContextRequest(String identityUrl, String credentialUrl, String participantDid, String participantContextId, String superUserApiKey, String dsp, String keySuffix) {
        var json = Json.createObjectBuilder()
                .add(ACTIVE, true)
                .add(PARTICIPANT_ID, participantDid)
                .add(PARTICIPANT_CONTEXT_ID, participantContextId)
                .add(DID, participantDid)
                .add(SCOPES, Json.createArrayBuilder())
                .add(SERVICE_ENDPOINTS, Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(TYPE, "CredentialService")
                                .add(SERVICE_ENDPOINT, "%s/v1/participants/%s".formatted(credentialUrl, participantContextId))
                                .add(ID, "%s-credentialservice-1".formatted(participantContextId)))
                        .add(Json.createObjectBuilder()
                                .add(TYPE, PROTOCOL_ENDPOINT)
                                .add(SERVICE_ENDPOINT, dsp)
                                .add(ID, "%s-dsp".formatted(participantContextId))))
                .add(KEY, Json.createObjectBuilder()
                        .add(KEY_ID, "%s#key-1".formatted(participantDid))
                        .add(PRIVATE_KEY_ALIAS, "%s#key-1".formatted(keySuffix))
                        .add(KEY_GENERATOR_PARAMS, Json.createObjectBuilder()
                                .add(ALGORITHM, EC)));

        // add key override for sts account keys to JSON object if the key was overwritten and not equal to the participantId
        if (!keySuffix.equals(participantDid)) {
            json.add(ADDITIONAL_PROPERTIES, Json.createObjectBuilder()
                    .add(CLIENT_SECRET, "%s-sts-client-secret".formatted(keySuffix))
            );
        }

        return new Request.Builder()
                .url("%s/v1beta/participants".formatted(identityUrl))
                .addHeader(X_API_KEY, superUserApiKey)
                .post(RequestBody.create(json.build().toString(), TYPE_JSON))
                .build();
    }

    public static Request buildGetCredentialTypeRequest(String identityUrl, String participantContextId, String superUserApiKey, String credentialType) {
        return new Request.Builder()
                .url("%s/v1beta/participants/%s/credentials?type=%s".formatted(identityUrl, participantContextId, credentialType))
                .addHeader(X_API_KEY, superUserApiKey)
                .build();
    }

    public static Request buildCreateMembershipCredentialRequest(String identityUrl, String participantContextId, String superUserApiKey, String issuerDid) {
        var json = Json.createObjectBuilder()
                .add(ISSUER_DID, issuerDid)
                .add(CREDENTIALS, Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(ID, MEMBERSHIP_CREDENTIAL_DEF)
                                .add(TYPE, MEMBERSHIP_CREDENTIAL)
                                .add(FORMAT, CREDENTIAL_FORMAT))
                )
                .build();

        return new Request.Builder()
                .url("%s/v1beta/participants/%s/credentials/request".formatted(identityUrl, participantContextId))
                .addHeader(X_API_KEY, superUserApiKey)
                .post(RequestBody.create(json.toString(), TYPE_JSON))
                .build();
    }

    public static Request buildCreateMarketPartnerCredentialRequest(String identityUrl, String participantContextId, String superUserApiKey, String issuerDid) {
        var json = Json.createObjectBuilder()
                .add(ISSUER_DID, issuerDid)
                .add(CREDENTIALS, Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(ID, MARKET_PARTNER_CREDENTIAL_DEF)
                                .add(TYPE, MARKET_PARTNER_CREDENTIAL)
                                .add(FORMAT, CREDENTIAL_FORMAT))
                )
                .build();

        return new Request.Builder()
                .url("%s/v1beta/participants/%s/credentials/request".formatted(identityUrl, participantContextId))
                .addHeader(X_API_KEY, superUserApiKey)
                .post(RequestBody.create(json.toString(), TYPE_JSON))
                .build();
    }
}
