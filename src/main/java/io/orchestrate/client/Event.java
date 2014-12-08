/*
 * Copyright 2013 the original author or authors.
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
 * A container for the event and its associated KV data.
 *
 * @param <T> The deserializable type for the value of the KV data belonging
 *            to this event.
 */
@ToString
@EqualsAndHashCode(callSuper = true)
public class Event<T> extends EventMetadata {

    private final ObjectMapper mapper;
    /** The value for this event. */
    private final T value;
    /** The raw JSON value for this event. */
    private String rawValue;

    Event(final ObjectMapper mapper, final String collection, final String key, final String type,
          final Long timestamp, final String ordinal, final String ref, final T value, final String rawValue) {
        super(collection, key, type, timestamp, ordinal, ref);
        this.mapper = mapper;
        this.value = value;
        // rawValue should not be 'final' b/c it will be lazy created when 'T' is not String
        this.rawValue = rawValue;
    }

    /**
     * Returns the KV object for this event.
     *
     * @return The KV object for this event.
     */
    public final T getValue() {
        return value;
    }

    /**
     * Returns the raw JSON value of this event.
     *
     * @return The raw JSON value of this event.
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
