package de.fraunhofer.iee.connector.controlplane.registration.util;

import jakarta.json.Json;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class RequestBuilder {
    private final static String X_API_KEY = "x-api-key";
    private static final String PROTOCOL_ENDPOINT = "ProtocolEndpoint";
    private static final MediaType TYPE_JSON = MediaType.parse("application/json");

    private static final String ACTIVE = "active";
    private static final String PARTICIPANT_ID = "participantId";
    private static final String DID = "did";
    private static final String ROLES = "roles";
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
    private static final String MEMBERSHIP_CREDENTIAL_DEF = "membership-credential-def-1";
    private static final String MEMBERSHIP_CREDENTIAL = "MembershipCredential";
    private static final String VC1_0_JWT = "VC1_0_JWT";

    public static Request buildGetParticipantContextRequest(String identityUrl, String participantIdB64, String superUserApiKey) {
        return new Request.Builder()
                .url("%s/v1alpha/participants/%s".formatted(identityUrl, participantIdB64))
                .addHeader(X_API_KEY, superUserApiKey)
                .build();
    }

    public static Request buildCreateParticipantContextRequest(String identityUrl, String credentialUrl ,String participantId, String participantIdB64, String superUserApiKey, String dsp, String keySuffix) {
        var json = Json.createObjectBuilder()
                .add(ACTIVE, true)
                .add(PARTICIPANT_ID, participantId)
                .add(DID, participantId)
                .add(ROLES, Json.createArrayBuilder())
                .add(SERVICE_ENDPOINTS, Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(TYPE, "CredentialService")
                                .add(SERVICE_ENDPOINT, "%s/v1/participants/%s".formatted(credentialUrl, participantIdB64))
                                .add(ID, "%s-credentialservice-1".formatted(participantId)))
                        .add(Json.createObjectBuilder()
                                .add(TYPE, PROTOCOL_ENDPOINT)
                                .add(SERVICE_ENDPOINT, dsp)
                                .add(ID, "%s-dsp".formatted(participantId))))
                .add(KEY, Json.createObjectBuilder()
                        .add(KEY_ID, "%s#key-1".formatted(participantId))
                        .add(PRIVATE_KEY_ALIAS, "%s#key-1".formatted(keySuffix))
                        .add(KEY_GENERATOR_PARAMS, Json.createObjectBuilder()
                                .add(ALGORITHM, EC)));

        // add key override for sts account keys to JSON object if the key was overwritten and not equal to the participantId
        if (!keySuffix.equals(participantId)) {
            json.add(ADDITIONAL_PROPERTIES, Json.createObjectBuilder()
                    .add(CLIENT_SECRET, "%s-sts-client-secret".formatted(keySuffix))
            );
        }

        return new Request.Builder()
                .url("%s/v1alpha/participants".formatted(identityUrl))
                .addHeader(X_API_KEY, superUserApiKey)
                .post(RequestBody.create(json.build().toString(), TYPE_JSON))
                .build();
    }

    public static Request buildGetMembershipCredentialRequest(String identityUrl, String participantIdB64, String superUserApiKey) {
        return new Request.Builder()
                .url("%s/v1alpha/participants/%s/credentials?type=MembershipCredential".formatted(identityUrl, participantIdB64))
                .addHeader(X_API_KEY, superUserApiKey)
                .build();
    }

    public static Request buildCreateMembershipCredentialRequest(String identityUrl, String participantIdB64, String superUserApiKey, String issuerDid) {
        var json = Json.createObjectBuilder()
                .add(ISSUER_DID, issuerDid)
                .add(CREDENTIALS, Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add(ID, MEMBERSHIP_CREDENTIAL_DEF)
                                .add(TYPE, MEMBERSHIP_CREDENTIAL)
                                .add(FORMAT, VC1_0_JWT))
                )
                .build();

        return new Request.Builder()
                .url("%s/v1alpha/participants/%s/credentials/request".formatted(identityUrl, participantIdB64))
                .addHeader(X_API_KEY, superUserApiKey)
                .post(RequestBody.create(json.toString(), TYPE_JSON))
                .build();
    }
}
