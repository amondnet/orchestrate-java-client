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

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class EventMetadata extends KvMetadata {
    private final Long timestamp;
    private final String ordinal;
    private final String type;

    EventMetadata(String collection, String key, String eventType, Long timestamp, String ordinal, String ref) {
        super(collection, key, ref);
        this.timestamp = timestamp;
        this.ordinal = ordinal;
        this.type = eventType;
    }

    public String getType() {
        return type;
    }

    /**
     * Returns the timestamp of this event.
     *
     * @return The timestamp for this event.
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the ordinal of this event.
     *
     * @return The ordinal for this event.
     */
    public String getOrdinal() {
        return ordinal;
    }
}
