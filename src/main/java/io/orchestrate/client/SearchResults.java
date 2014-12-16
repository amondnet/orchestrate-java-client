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

import java.util.Iterator;
import java.util.List;

/**
 * A container for the results from a search query.
 *
 * @param <T> The deserializable type for the KV objects in the search results.
 */
@ToString
@EqualsAndHashCode
public class SearchResults<T> implements Iterable<Result<T>> {

    /** The results of this search. */
    private final List<Result<T>> results;
    /** The total number of search results. */
    private final int totalCount;
    /** The aggregate results associated with this search. */
    private final List<AggregateResult> aggregates;

    SearchResults(final List<Result<T>> results, final int totalCount, final List<AggregateResult> aggregates) {
        assert (results != null);
        assert (totalCount >= 0);

        this.results = results;
        this.totalCount = totalCount;
        this.aggregates = aggregates;
    }

    /**
     * Returns the search results for this request.
     *
     * @return The search results.
     */
    public final Iterable<Result<T>> getResults() {
        return results;
    }

    /**
     * Returns the aggregate results associated with these search results.
     *
     * @return The aggregate results.
     */
    public final Iterable<AggregateResult> getAggregates() {
        return aggregates;
    }

    /**
     * Returns the number of search results.
     *
     * @return The number of search results.
     */
    public final int getCount() {
        return results.size();
    }

    /**
     * Returns the total count of search results.
     *
     * @return The total number of results for the query.
     */
    public final int getTotalCount() {
        return totalCount;
    }

    /** {@inheritDoc} */
    @Override
    public final Iterator<Result<T>> iterator() {
        return results.iterator();
    }

}
