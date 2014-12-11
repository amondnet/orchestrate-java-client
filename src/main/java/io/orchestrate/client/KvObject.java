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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A container for a KV object.
 *
 * @param <T> The deserializable type for the value of this KV object.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class KvObject<T> extends KvMetadata {
    private final ObjectMapper mapper;

    /** The value for this KV object. */
    private final T value;
    /** The raw JSON value for this KV object. */
    private String rawValue;

    KvObject(final ObjectMapper mapper, final KvMetadata metadata, final T value, final String rawValue) {
        super(metadata.getCollection(), metadata.getKey(), metadata.getRef());
        this.mapper = mapper;
        this.value = value;
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
     * Returns the raw JSON value of this KV object.
     *
     * @return The raw JSON value of this KV object, may be {@code null}.
     */
    public final String getRawValue() {
        if (rawValue == null && value != null) {
            try {
                rawValue = mapper.writeValueAsString(value);
            } catch (JsonProcessingException ignored) {
            }
        }
        return rawValue;
    }

}
