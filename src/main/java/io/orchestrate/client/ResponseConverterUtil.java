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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * A utility class with helper methods for converting JSON response data from
 * Orchestrate.
 */
final class ResponseConverterUtil {

    static <T> KvObject<T> jsonToKvObject(
            final ObjectMapper mapper, final JsonNode jsonNode, final Class<T> clazz)
            throws IOException {
        assert (mapper != null);
        assert (jsonNode != null);
        assert (clazz != null);

        // parse the PATH structure (e.g.):
        // {"collection":"coll","key":"aKey","ref":"someRef"}
        final JsonNode path = jsonNode.get("path");
        final String collection = path.get("collection").asText();
        final String key = path.get("key").asText();
        final String ref = path.get("ref").asText();

        // parse result structure (e.g.):
        // {"path":{...},"value":{}}
        final JsonNode valueNode = jsonNode.get("value");

        return jsonToKvObject(mapper, valueNode, clazz, collection, key, ref);
    }

    @SuppressWarnings("unchecked")
    public static <T> KvObject<T> jsonToKvObject(ObjectMapper mapper, JsonNode valueNode, Class<T> clazz,
                                                 String collection, String key, String ref) throws IOException {
        assert (mapper != null);
        assert (clazz != null);

        final KvMetadata metadata = new KvMetadata(collection, key, ref);

        final T value = jsonToDomainObject(mapper, valueNode, clazz);
        String rawValue = null;

        if (value != null && value instanceof String) {
            rawValue = (String)value;
        }

        return new KvObject<T>(mapper, metadata, value, rawValue);
    }

    @SuppressWarnings("unchecked")
    public static <T> KvObject<T> jsonToKvObject(ObjectMapper mapper, String rawValue, Class<T> clazz,
                                                 String collection, String key, String ref) throws IOException {
        assert (mapper != null);
        assert (clazz != null);

        final KvMetadata metadata = new KvMetadata(collection, key, ref);

        final T value = jsonToDomainObject(mapper, rawValue, clazz);

        return new KvObject<T>(mapper, metadata, value, rawValue);
    }

    @SuppressWarnings("unchecked")
    public static <T> T jsonToDomainObject(ObjectMapper mapper,
                       String rawValue, Class<T> clazz) throws IOException {
        if (rawValue != null && !rawValue.isEmpty()) {
            if (clazz.equals(String.class) ){
                return (T)rawValue;
            }
            return mapper.readValue(rawValue, clazz);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T jsonToDomainObject(ObjectMapper mapper,
                                           JsonNode json, Class<T> clazz) throws IOException {
        if (json != null && !json.isNull()) {
            if (clazz.equals(String.class) ){
                return (T)mapper.writeValueAsString(json);
            }
            return mapper.treeToValue(json, clazz);
        }
        return null;
    }

    public static <T> Event<T> wrapperJsonToEvent(ObjectMapper mapper, JsonNode wrapperJson, Class<T> clazz) throws IOException {
        assert (mapper != null);
        assert (clazz != null);

        final JsonNode path = wrapperJson.get("path");

        final String collection = path.get("collection").textValue();
        final String key = path.get("key").textValue();
        final String eventType = path.get("type").textValue();
        final String ref = path.get("ref").textValue();

        final long timestamp = path.get("timestamp").longValue();
        final String ordinal = path.get("ordinal").asText();

        final JsonNode valueNode = wrapperJson.get("value");

        final T value = jsonToDomainObject(mapper, valueNode, clazz);
        String rawValue = null;
        if(value != null && value instanceof String) {
            rawValue = (String)value;
        }

        return new Event<T>(mapper, collection, key, eventType, timestamp, ordinal, ref, value, rawValue);

    }
}
