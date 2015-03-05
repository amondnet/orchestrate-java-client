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

import java.io.IOException;

/**
 * A utility class with helper methods for converting JSON response data from
 * Orchestrate.
 */
final class ResponseConverterUtil {

    static <T> KvObject<T> wrapperJsonToKvObject(
            final ObjectMapper mapper, final JsonNode jsonNode, final Class<T> clazz)
            throws IOException {
        assert (mapper != null);
        assert (jsonNode != null);
        assert (clazz != null);

        // parse the PATH structure (e.g.):
        // {"collection":"coll","key":"aKey","ref":"someRef"}
        final JsonNode path = jsonNode.get("path");
        if ("event".equals(path.get("kind").asText())) {
            return wrapperJsonToEvent(mapper, jsonNode, clazz);
        }

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

        final T value = jsonToDomainObject(mapper, valueNode, clazz);
        String rawValue = null;

        if (value != null && value instanceof String) {
            rawValue = (String)value;
        }

        return new KvObject<T>(collection, key, ref, mapper, value, valueNode, rawValue);
    }

    @SuppressWarnings("unchecked")
    public static <T> KvObject<T> jsonToKvObject(ObjectMapper mapper, String rawValue, Class<T> clazz,
                                                 String collection, String key, String ref) throws IOException {
        assert (mapper != null);
        assert (clazz != null);

        JsonNode valueNode = null;
        if (rawValue != null && !rawValue.isEmpty()) {
            valueNode = mapper.readTree(rawValue);
        }

        final T value = jsonToDomainObject(mapper, valueNode, rawValue, clazz);

        return new KvObject<T>(collection, key, ref, mapper, value, valueNode, rawValue);
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public static <T> T jsonToDomainObject(ObjectMapper mapper,
                       String rawValue, Class<T> clazz) throws IOException {
        if (clazz == null || clazz == Void.class || rawValue == null || rawValue.isEmpty()) {
            return null;
        }

        if (clazz.equals(String.class) ){
            return (T)rawValue;
        }
        return mapper.readValue(rawValue, clazz);
    }

    @SuppressWarnings("unchecked")
    static <T> T jsonToDomainObject(ObjectMapper mapper,
                                           JsonNode json, String rawValue, Class<T> clazz) throws IOException {
        if (clazz == null || clazz == Void.class || json == null || json.isNull()) {
            return null;
        }

        if (clazz.equals(String.class) ){
            if (rawValue != null) {
                return (T)rawValue;
            }
            return (T)mapper.writeValueAsString(json);
        }
        return mapper.treeToValue(json, clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> T jsonToDomainObject(ObjectMapper mapper,
                                           JsonNode json, Class<T> clazz) throws IOException {
        if (clazz == null || clazz == Void.class || json == null || json.isNull()) {
            return null;
        }

        if (clazz.equals(String.class) ){
            return (T)mapper.writeValueAsString(json);
        }
        return mapper.treeToValue(json, clazz);
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

        return new Event<T>(mapper, collection, key, eventType, timestamp, ordinal, ref, value, valueNode, rawValue);
    }
}
