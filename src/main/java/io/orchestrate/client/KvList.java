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

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

/**
 * A container for the KV objects with results.
 *
 * @param <T> The deserializable type for the KV objects in the results.
 */
@ToString
@EqualsAndHashCode
public class KvList<T> implements Iterable<KvObject<T>> {

    /** The KV objects from the KV listing request. */
    private final List<KvObject<T>> results;
    /** The total number of KV objects. */
    private final int count;
    /** The next page of the results URL specified. */
    private final OrchestrateRequest<KvList<T>> next;

    KvList(final List<KvObject<T>> results, final int count, @Nullable final OrchestrateRequest<KvList<T>> next) {
        assert (results != null);
        assert (count >= 0);

        this.results = results;
        this.count = count;
        this.next = next;
    }

    /**
     * Returns the KV objects for this request.
     *
     * @return The KV objects.
     */
    public final Iterable<KvObject<T>> getResults() {
        return results;
    }

    /**
     * Returns the number of KV objects in the results.
     *
     * @return The number of KV objects.
     */
    public final int getCount() {
        return count;
    }

    /**
     * The Request for fetching the next page of the KV objects.
     *
     * @return The OrchestrateRequest that will fetch the next page of KV objects.
     */
    @Nullable
    public final OrchestrateRequest<KvList<T>> getNext() {
        return next;
    }

    /**
     * Whether there is a URL for the next page of the KV objects.
     *
     * @return The URL of the next page of KV objects.
     */
    public final boolean hasNext() {
        return (next != null);
    }

    /** {@inheritDoc} */
    @Override
    public final Iterator<KvObject<T>> iterator() {
        return results.iterator();
    }

}
