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

import java.io.IOException;

/**
 * A container for a KV object.
 *
 * @param <T> The deserializable type for the value of this KV object.
 */
@ToString
@EqualsAndHashCode
public class KvObject<T> implements KvMetadata {
    private final ObjectMapper mapper;

    /** The collection for this KV metadata. */
    private final String collection;
    /** The key for this metadata. */
    private final String key;
    /** The version for this metadata. */
    private final String ref;

    /** The value for this KV object. */
    private final T value;
    /** The raw JSON value for this KV object. */
    private String rawValue;
    private JsonNode valueNode;

    KvObject(final String collection, final String key, final String ref,
             final ObjectMapper mapper, final T value,
             final JsonNode valueNode, final String rawValue) {
        assert (key != null);
        assert (key.length() > 0);
        assert (ref != null);
        assert (ref.length() > 0);

        this.collection = collection;
        this.key = key;
        this.ref = ref;

        this.mapper = mapper;
        this.value = value;
        this.valueNode = valueNode;
        this.rawValue = rawValue;
    }

    /**
     * Returns the value of this KV object.
     *
     * @return The value of the KV object, may be {@code null}.
     */
    public final T getValue() {
        return value;
    }

    /**
     * Returns the value of this KV object, mapped to the provided Class.
     * This is useful in cases where results may be of mixed types (eg
     * if a search request includes items and events). The original value
     * json will be deserialized to the provided type.
     *
     * <p>
     * To get the value as the raw json, call with String.class
     * </p>
     * <pre>
     * {@code
     * String json = kv.getValue(String.class);
     * }
     * </pre>
     * <p>
     * This is equivalent to calling getRawValue. Other common use cases are
     * Map.class (to just get the json as a nested hashmap), or a POJO class
     * of your own (for example a simple User java bean class).
     * </p>
     *
     * @return The value of the KV object, may be {@code null}.
     */
    public <T> T getValue(Class<T> clazz) {
        if (valueNode == null) {
            return null;
        }

        if (rawValue == null && value != null && clazz.equals(String.class)) {
            rawValue = (String)value;
        }

        try {
            return ResponseConverterUtil.jsonToDomainObject(mapper, valueNode, rawValue, clazz);
        } catch (IOException e) {
            throw new ClientException("Could not convert response to JSON.", e);
        }
    }

    /**
     * Returns the raw JSON value of this KV object.
     *
     * @return The raw JSON value of this KV object, may be {@code null}.
     */
    public final String getRawValue() {
        if (rawValue == null) {
            if (valueNode != null) {
                rawValue = getValue(String.class);
            } else if (value != null) {
                try {
                    rawValue = mapper.writeValueAsString(value);
                } catch (JsonProcessingException ignored) {
                }
            }
        }
        return rawValue;
    }

    @Override
    public String getCollection() {
        return collection;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getRef() {
        return ref;
    }
}
