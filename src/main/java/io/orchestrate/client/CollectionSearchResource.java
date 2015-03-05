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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Method;

import java.io.IOException;
import java.util.*;

import static io.orchestrate.client.Preconditions.*;

/**
 * The resource for the collection search features in the Orchestrate API.
 */
public class CollectionSearchResource extends BaseResource {
    private static final String QUERY_WITH_KIND = "@path.kind:(%s) AND (%s)";

    /** The name of the collection to search. */
    private final String collection;
    /** The number of search results to retrieve. */
    private int limit;
    /** The offset to start search results at. */
    private int offset;
    /** Whether to retrieve the values for the list of search results. */
    private boolean withValues;
    /** The fully-qualified names of fields to sort upon. */
    private String sortFields;
    /** The aggregate functions to include in this query. */
    private String aggregateFields;
    /** The 'kinds' to be searched ('item' or 'event') */
    private String kinds;

    CollectionSearchResource(
            final OrchestrateClient client,
            final JacksonMapper mapper,
            final String collection) {
        super(client, mapper);
        assert (collection != null);
        assert (collection.length() > 0);

        this.collection = collection;
        this.limit = 10;
        this.offset = 0;
        this.withValues = true;
        this.sortFields = null;
        this.aggregateFields = null;
        this.kinds = null;
    }

    /**
     * Send the search request to Orchestrate, without deserializing the Values. This is
     * useful for cases where the results may be of mixed types (eg if the search includes
     * items and events in the same query). The Result values can be individually
     * deserialized (perhaps based on some indicator in the KvMetadata) by calling
     * {@link KvObject#getValue(Class clazz) KvObject.getValue}
     * <p>Usage:</p>
     * <pre>
     * {@code
     * String luceneQuery = "*";
     * SearchResults&lt;Void&gt; results1 =
     *         client.searchCollection("someCollection")
     *               .sort("value.name.last:asc")
     *               .get(luceneQuery)
     *               .get();
     * }
     * for (Result&lt;Void&gt; result : results1.getResults()) {
     *     // gets the results 'value' as a String, which will be the json string.
     *     System.out.println(result.getValue(String.class));
     * }
     * }
     * </pre>
     *
     * @param luceneQuery The lucene search query.
     * @return The prepared search request.
     */
    public OrchestrateRequest<SearchResults<Void>> get(final String luceneQuery) {
        return get(Void.class, luceneQuery);
    }

    /**
     * Retrieve data from the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * String luceneQuery = "*";
     * SearchResults<String> results1 =
     *         client.searchCollection("someCollection")
     *               .limit(10)
     *               .offset(0)
     *               .sort("value.name.last:asc")
     *               .get(String.class, luceneQuery)
     *               .get();
     * }
     * </pre>
     *
     * @param clazz Type information for marshalling objects at runtime.
     * @param luceneQuery The lucene search query.
     * @param <T> The type to
     * @return The prepared search request.
     */
    public <T> OrchestrateRequest<SearchResults<T>> get(
            final Class<T> clazz, final String luceneQuery) {
        checkNotNull(clazz, "clazz");
        checkNotNullOrEmpty(luceneQuery, "luceneQuery");

        StringBuilder buff = new StringBuilder("query=");
        if (this.kinds != null) {
            buff.append(client.encode(String.format(QUERY_WITH_KIND, this.kinds, luceneQuery)));
        } else {
            buff.append(client.encode(luceneQuery));
        }
        buff.append("&limit=").append(limit)
            .append("&offset=").append(offset)
            .append("&values=").append(Boolean.toString(withValues));
        if (sortFields != null) {
            buff.append("&sort=").append(client.encode(sortFields));
        }
        if (aggregateFields != null) {
            buff.append("&aggregate=").append(client.encode(aggregateFields));
        }

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.GET)
                .uri(client.uri(collection))
                .query(buff.toString())
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<SearchResults<T>>(client, packet, new ResponseConverter<SearchResults<T>>() {
            @Override
            public SearchResults<T> from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                assert (status == 200);

                final JsonNode jsonNode = toJsonNode(response);

                final int totalCount = jsonNode.get("total_count").asInt();
                final int count = jsonNode.get("count").asInt();
                final List<Result<T>> results = new ArrayList<Result<T>>(count);

                List<AggregateResult> aggregates = Collections.<AggregateResult>emptyList();
                if (jsonNode.has("aggregates")) {
                    aggregates = AggregateResult.from((ArrayNode) jsonNode.get("aggregates"));
                }

                final Iterator<JsonNode> iter = jsonNode.get("results").elements();
                while (iter.hasNext()) {
                    final JsonNode result = iter.next();

                    // parse result structure (e.g.):
                    // {"path":{...},"value":{},"score":1.0}
                    final double score = (result.get("score") != null)
                            ? result.get("score").asDouble(0)
                            : 0.0;
                    final Double distance = (result.get("distance") != null)
                            ? result.get("distance").asDouble(0)
                            : null;
                    final KvObject<T> kvObject = toKvObject(result, clazz);

                    results.add(new Result<T>(kvObject, score, distance));
                }

                return new SearchResults<T>(results, totalCount, aggregates);
            }
        });
    }

    /**
     * The number of search results to get in this query, this value cannot
     * exceed 100.
     *
     * @param limit The number of search results in this query.
     * @return This request.
     */
    public CollectionSearchResource limit(final int limit) {
        this.limit = checkNotNegative(limit, "limit");
        return this;
    }

    /**
     * The position in the results list to start retrieving results from,
     * this is useful for paginating results.
     *
     * @param offset The position to start retrieving results from.
     * @return This request.
     */
    public CollectionSearchResource offset(final int offset) {
        this.offset = checkNotNegative(offset, "offset");
        return this;
    }

    /**
     * Apply sorting to the results in the search query, this will render the
     * relevancy value in the "score" field for the results to be "0.0". To
     * specify multiple sort-field names use "," to separate names, in which
     * case the secondary fields will be used as tie-breakers.
     *
     * <p>
     * {@code
     * client.searchCollection("someCollection")
     *     .sort("value.name.last:asc,value.name.first:asc")
     *     .get(String.class, "*")
     *     .get()
     * }
     * </p>
     *
     * @param sortFields The comma separated field names to sort by.
     * @return This request.
     */
    public CollectionSearchResource sort(final String sortFields) {
        this.sortFields = checkNotNull(sortFields, "sortFields");
        return this;
    }

    /**
    * Apply a collection of aggregate functions to the items matched by this
    * query. Multiple different aggregate functions can be included, by separating
    * them with commas.
    *
    * <p>
    * {@code
    * client.searchCollection("someCollection")
    *     .aggregate("value.cart.items.price:stats,value.inventory.in_stock:range:0~10:10~100:100~*")
    *     .get(String.class, "*")
    *     .get()
    * }
    * </p>
    *
    * The complete list of aggregate functions is passed into this method as a string.
    * To make sure you use correct syntax, we recommend using the Aggregate class,
    * with its builder functionality.
    *
    * <p>
    * {@code
    * client.searchCollection("someCollection")
    *     .aggregate(Aggregate.builder()
    *         .stats("value.cart.items.price")
    *         .range(
    *             "value.inventory.in_stock",
    *             Range.between(0, 10),
    *             Range.between(10, 100),
    *             Range.above(100)
    *         )
    *         .build()
    *     )
    *     .get(String.class, "*")
    *     .get()
    * }
    * </p>
    *
    * @param aggregateFields The comma separated aggregate-function definitions.
    * @return This request.
    */
   public CollectionSearchResource aggregate(final String aggregateFields) {
       this.aggregateFields = checkNotNull(aggregateFields, "aggregateFields");
       return this;
   }

    /**
     * If {@code withValues} is {@code true} then the KV objects in the search
     * results will be retrieved with their values. Defaults to {@code true}.
     *
     * @param withValues The setting for whether to retrieve KV values.
     * @return This request.
     */
    public CollectionSearchResource withValues(final boolean withValues) {
        this.withValues = withValues;
        return this;
    }

    /**
     * Specify what 'kind' of objects to query. This is a helper method to set up the
     * '@path.kind' predicate in the query. By default, no @path.kind predicate is used,
     * and Orchestrate currently defaults to returning only 'item's. Currently, only
     * 'item' and/or 'event' can be specified.
     *
     * @param kinds The kinds to query (valid values are currently 'item', and 'event').
     * @return This request.
     */
    public CollectionSearchResource kinds(final String...kinds) {
        if (kinds == null || kinds.length == 0){
            this.kinds = null;
            return this;
        }

        boolean includeEvent = false;
        boolean includeItem = false;
        for (String kind : kinds) {
            if ("item".equals(kind)) {
                includeItem = true;
            } else if("event".equals(kind)) {
                includeEvent = true;
            } else {
                throw new IllegalArgumentException("'kind' must be one of [item, event].");
            }
        }

        if (includeItem && includeEvent) {
            this.kinds = "item event";
        } else if(includeItem) {
            this.kinds = "item";
        } else {
            this.kinds = "event";
        }
        return this;
    }
}
