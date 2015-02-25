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

/**
 * A container for metadata about a KV object.
 */
public interface KvMetadata {
    /**
     * Returns the collection this metadata belongs to.
     *
     * @return The collection of this metadata.
     */
    String getCollection();

    /**
     * Returns the key of this metadata.
     *
     * @return The key for this metadata.
     */
    String getKey();

    /**
     * Returns the reference (i.e. "version") of this metadata.
     *
     * @return The reference for this metadata.
     */
    String getRef();
}
