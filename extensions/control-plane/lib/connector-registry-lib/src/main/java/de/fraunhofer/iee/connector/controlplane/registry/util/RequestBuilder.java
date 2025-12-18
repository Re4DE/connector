package de.fraunhofer.iee.connector.controlplane.registry.util;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.Request;
import okhttp3.RequestBody;

public class RequestBuilder {
    private final static String X_API_KEY = "x-api-key";
    private final static String GET = "GET";

    private final static String NAME = "name";
    private final static String ID = "id";
    private final static String URL = "url";
    private final static String SUPPORTED_PROTOCOLS = "supportedProtocols";
    private final static String DATASPACE_PROTOCOL_HTTP = "dataspace-protocol-http";


    public static Request buildPostRequest(String url, String apiKey, RequestBody body) {
        return new Request.Builder()
                .url(url)
                .header(X_API_KEY, apiKey)
                .post(body)
                .build();
    }

    public static Request buildGetRequest(String url, String apiKey) {
        return new Request.Builder()
                .url(url)
                .header(X_API_KEY, apiKey)
                .method(GET, null)
                .build();
    }

    public static JsonObject buildPostRequestBody(String connectorName, String participantId, String dsp) {
        return Json.createObjectBuilder()
                .add(NAME, connectorName)
                .add(ID, participantId)
                .add(URL, dsp)
                .add(SUPPORTED_PROTOCOLS, Json.createArrayBuilder().add(DATASPACE_PROTOCOL_HTTP))
                .build();
    }
}
