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

/**
 * A container for metadata about a graph Relationship object.
 */
public interface RelationshipMetadata {

    /**
     * Returns the source collection for this relationship.
     *
     * @return The source collection for this relationship.
     */
    String getSourceCollection();

    /**
     * Returns the source key for this relationship.
     *
     * @return The source key for this relationship.
     */
    String getSourceKey();

    /**
     * Returns the source collection for this relationship.
     *
     * @return The source collection for this relationship.
     */
    String getDestinationCollection();

    /**
     * Returns the destination key for this relationship.
     *
     * @return The destination key for this relationship.
     */
    String getDestinationKey();

    /**
     * Returns the relation label for this relationship.
     *
     * @return The relation label for this relationship.
     */
    String getRelation();

    /**
     * Returns the reference (i.e. "version") of this relationship.
     *
     * @return The reference for this relationship.
     */
    String getRef();
}
