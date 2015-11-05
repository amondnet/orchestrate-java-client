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

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

/**
 * A container for relationship objects.
 */
public class RelationshipList<T> implements Iterable<KvObject<T>> {

    private final List<KvObject<T>> relatedObjects;

    /** The next page of the results URL specified. */
    private final OrchestrateRequest<RelationshipList<T>> next;

    RelationshipList(final List<KvObject<T>> relatedObjects, @Nullable final OrchestrateRequest<RelationshipList<T>> next) {
        assert (relatedObjects != null);

        this.relatedObjects = relatedObjects;
        this.next = next;
    }

    /**
     * Returns the related objects from the response.
     *
     * @return The related objects.
     */
    public final Iterable<KvObject<T>> getRelatedObjects() {
        return relatedObjects;
    }

    /**
     * The URL for the next page of the KV objects.
     *
     * @return The URL of the next page of KV objects.
     */
    @Nullable
    public final OrchestrateRequest<RelationshipList<T>> getNext() {
        return next;
    }

    /**
     * Whether there is a URL for the next page of the graph results.
     *
     * @return The URL of the next page of graph results.
     */
    public final boolean hasNext() {
        return (next != null);
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<KvObject<T>> iterator() {
        return relatedObjects.iterator();
    }

}
