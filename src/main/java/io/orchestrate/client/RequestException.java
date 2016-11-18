/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.orchestrate.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

/**
 * An object that stores information about a failed {@code Client} request.
 */
@ToString
@EqualsAndHashCode(callSuper=false)
@SuppressWarnings("serial")
public class RequestException extends ClientException {
    protected static ObjectMapper MAPPER = new ObjectMapper();

    /** The HTTP status code from the request. */
    private final int statusCode;
    /** The HTTP response ID from the request. */
    private final String requestId;

    private final String rawResponse;

    private Map details;
    private String locator;
    private String info;

    RequestException(final int statusCode, final JsonNode json, final String rawResponse, final String requestId) {
        super(getMessageFromJson(json, rawResponse));
        assert (statusCode >= 0);
        assert (requestId != null);
        assert (requestId.length() > 0);

        this.rawResponse = rawResponse;
        this.statusCode = statusCode;
        this.requestId = requestId;
        if(json != null) {
            if(json.has("details")) {
                try {
                    details = MAPPER.treeToValue(json.get("details"), Map.class);
                    if(details.containsKey("info")) {
                        info = (String)details.get("info");
                    }
                } catch (JsonProcessingException ignored) {
                }
            }
            if(json.has("locator")) {
                this.locator = json.get("locator").textValue();
            }
        }
    }

    private static String getMessageFromJson(JsonNode node, String rawResponse) {
        if(node == null) {
            return rawResponse;
        }
        if(node.has("message")) {
            return node.get("message").asText();
        }
        return rawResponse;
    }

    /**
     * Returns the HTTP status code from the failed request.
     *
     * @return The HTTP status code from the failed request.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the HTTP response ID from the failed request.
     *
     * <p>The response ID is used to help with debugging the query in the
     * Orchestrate.io service.</p>
     *
     * @return The HTTP response ID from the failed request.
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * If the error from Orchestrate includes a details map, it will be exposed here.
     * @return The details map from the response, or null if there is not one.
     */
    public Map getDetails() {
        return details;
    }

    /**
     * The locator code in the response, if present. The locator code can be useful to help with
     * debugging when working with Orchestrate support.
     *
     * @return The locator code from the response, null if not present.
     */
    public String getLocator() {
        return locator;
    }

    /**
     * Returns the raw response body that the Orc service returned. This is useful
     * for cases where the orc service introduces new fields in error responses that
     * the client version currently in use does not explicitly expose.
     *
     * @return The raw response body. This will usually be JSON, but in some unexpected
     * cases it may be html or plain text.
     */
    public String getRawResponse() {
        return rawResponse;
    }

    /**
     * 'info' is a commonly included detail for Orchestrate errors. Exposed here for convenience.
     * @return The details.info entry, if present.
     */
    public String getInfo() {
        return info;
    }
}
