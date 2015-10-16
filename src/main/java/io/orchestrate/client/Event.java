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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A container for the event and its associated KV data.
 *
 * @param <T> The deserializable type for the value of the KV data belonging
 *            to this event.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Event<T> extends KvObject<T> implements EventMetadata {
    private final Long timestamp;
    private final String ordinal;
    private final String type;

    Event(final ObjectMapper mapper, final String collection, final String key, final String type,
          final Long timestamp, final String ordinal, final String ref, final Long reftime, final T value,
          final JsonNode valueNode, final String rawValue) {
        super(collection, key, ref, reftime, mapper, value, valueNode, rawValue);
        this.timestamp = timestamp;
        this.ordinal = ordinal;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getOrdinal() {
        return ordinal;
    }
}
