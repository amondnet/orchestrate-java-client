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

import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A container for a graph relationship and its associated data.
 *
 * @param <T> The deserializable type for the value of the data belonging
 *            to this event.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Relationship<T> extends KvObject<T> implements RelationshipMetadata {

    private final String destinationCollection;
    private final String destinationKey;
    private final String relation;

    Relationship(
        final ObjectMapper mapper,
        final String collection, final String key,
        final String relation,
        final String destinationCollection, final String destinationKey,
        final String ref, final Long reftime,
        final T value, final JsonNode valueNode, final String rawValue
    ) {
        super(collection, key, ref, reftime, mapper, value, valueNode, rawValue);
        this.destinationCollection = destinationCollection;
        this.destinationKey = destinationKey;
        this.relation = relation;
    }

    /**
     * Returns the ItemKind of this object
     */
    @Override
    public ItemKind getItemKind() {
        return ItemKind.RELATIONSHIP;
    }

    @Override
    public String getSourceCollection() {
        return getCollection();
    }

    @Override
    public String getSourceKey() {
        return getKey();
    }

    @Override
    public String getDestinationCollection() {
        return destinationCollection;
    }

    @Override
    public String getDestinationKey() {
        return destinationKey;
    }

    @Override
    public String getRelation() {
        return relation;
    }
}
