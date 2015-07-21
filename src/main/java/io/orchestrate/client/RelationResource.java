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

import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.memory.ByteBufferWrapper;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;
import static io.orchestrate.client.Preconditions.*;

/**
 * The resource for the relation features in the Orchestrate API.
 */
public class RelationResource extends BaseResource {

    /** The collection containing the source key. */
    private String sourceCollection;
    /** The source key to add the relation to. */
    private String sourceKey;
    /** The collection containing the destination key. */
    private String destCollection;
    /** The destination key to add the relation to. */
    private String destKey;
    /** Whether to store the object if no key already exists. */
    private boolean ifAbsent;
    /** The last known version of the stored object. */
    private String objectRef;
    /** Whether to swap the "source" and "destination" objects. */
    private boolean invert;
    /** The number of graph results to retrieve. */
    private int limit;
    /** The offset to start graph results at. */
    private int offset;

    RelationResource(final OrchestrateClient client,
            final JacksonMapper mapper,
            final String sourceCollection,
            final String sourceKey) {
        super(client, mapper);
        assert (sourceCollection != null);
        assert (sourceKey != null);

        this.sourceCollection = sourceCollection;
        this.sourceKey = sourceKey;
        this.ifAbsent = false;
        this.objectRef = null;
        this.limit = 10;
        this.offset = 0;
    }

    /**
     * Equivalent to {@code this.ifAbsent(Boolean.TRUE)}.
     *
     * @return This RelationResource.
     * @see #ifAbsent(boolean)
     */
    public RelationResource ifAbsent() {
        return ifAbsent(Boolean.TRUE);
    }

    /**
     * Whether to store the object if no such relation already exists.
     *
     * @param ifAbsent If {@code true}
     * @return This RelationResource.
     */
    public RelationResource ifAbsent(final boolean ifAbsent) {
        checkArgument(!ifAbsent || objectRef == null, "'ifMatch' and 'ifAbsent' cannot be used together.");

        this.ifAbsent = ifAbsent;
        return this;
    }

    /**
     * The last known version of the stored object to match for the request to
     * succeed.
     *
     * @param objectRef The last known version of the stored object.
     * @return This RelationResource.
     */
    public RelationResource ifMatch(final @NonNull String objectRef) {
        checkArgument(!ifAbsent, "'ifMatch' and 'ifAbsent' cannot be used together.");

        this.objectRef = objectRef;
        return this;
    }

    /**
     * Retrieve a Relation object.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * Relation relation =
     *         client.relation("someCollection", "someKey")
     *               .get("someKind", "otherCollection", "otherKey")
     *               .get();
     * }
     * </pre>
     *
     * @param kind String The name of the relationship.
     * @param destCollection String The name of destination collection.
     * @param destKey String The name of destination key.
     * @return A prepared get request.
     */
    public OrchestrateRequest<Relation> get(final String kind, final String destCollection, final String destKey) {

        checkNotNullOrEmpty(kind, "kind");
        checkNotNullOrEmpty(destCollection, "destCollection");
        checkNotNullOrEmpty(destKey, "destKey");

        final String uri = client.uri(sourceCollection, sourceKey, "relation", kind, destCollection, destKey);

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.GET)
                .uri(uri)
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<Relation>(client, packet, new ResponseConverter<Relation>() {
            @Override
            public Relation from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();
                final int status = ((HttpResponsePacket) header).getStatus();
                assert (status == 200 || status == 404);

                if (status == 404) {
                    return null;
                }

                final String ref = header.getHeader(Header.ETag)
                        .replace("\"", "")
                        .replace("-gzip", "");
                JsonNode value = toJsonNodeOrNull(response);

                return new Relation(
                    sourceCollection, sourceKey,
                    destCollection, destKey, kind,
                    ref, value
                );
            }
        });
    }

    /**
     * Fetch objects related to a key in the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * RelationList<String> relatedObjects =
     *         client.relation("someCollection", "someKey")
     *               .get(String.class, "someKind")
     *               .get();
     * }
     * </pre>
     *
     * @param clazz Type information for deserializing to type {@code T} at
     *              runtime.
     * @param kinds The name of the relationships to traverse to the related
     *              objects.
     * @param <T> The type to deserialize the response from the request to.
     * @return A prepared get request.
     */
    public <T> OrchestrateRequest<RelationList<T>> get(final Class<T> clazz, final String... kinds) {
        checkNotNull(clazz, "clazz");
        checkArgument(destCollection == null && destKey == null,
                "'destCollection' and 'destKey' not valid in GET query.");
        checkNoneEmpty(kinds, "kinds", "kind");

        final String uri = client.uri(sourceCollection, sourceKey, "relations").concat("/" + client.encode(kinds));

        final String query = "limit=".concat(limit + "")
                .concat("&offset=").concat(offset + "");

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.GET)
                .uri(uri)
                .query(query)
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<RelationList<T>>(client, packet, new ResponseConverter<RelationList<T>>() {
            @Override
            public RelationList<T> from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                assert (status == 200 || status == 404);

                if (status == 404) {
                    return null;
                }

                final JsonNode jsonNode = toJsonNode(response);

                final OrchestrateRequest<RelationList<T>> next;
                if (jsonNode.has("next")) {
                    final String page = jsonNode.get("next").asText();
                    final URI url = URI.create(page);
                    final HttpContent packet = HttpRequestPacket.builder()
                            .method(Method.GET)
                            .uri(uri)
                            .query(url.getQuery())
                            .build()
                            .httpContentBuilder()
                            .build();
                    next = new OrchestrateRequest<RelationList<T>>(client, packet, this, false);
                } else {
                    next = null;
                }

                final int count = jsonNode.path("count").asInt();
                final List<KvObject<T>> relatedObjects = new ArrayList<KvObject<T>>(count);

                for (JsonNode node : jsonNode.path("results")) {
                    relatedObjects.add(toKvObject(node, clazz));
                }

                return new RelationList<T>(relatedObjects, next);
            }
        });
    }

    /**
     * Store a relationship between two objects in the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * boolean result =
     *         client.relation("someCollection", "someKey")
     *               .to("anotherCollection", "anotherKey")
     *               .put(kind)
     *               .get();
     * }
     * </pre>
     *
     * @param kind The name of the relationship to create.
     * @return A prepared put request.
     */
    public OrchestrateRequest<Boolean> put(final String kind) {
        HttpContent packet = prepareCreateRelation(kind, null);
        return new OrchestrateRequest<Boolean>(client, packet, new ResponseConverter<Boolean>() {
            @Override
            public Boolean from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();
                final int status = ((HttpResponsePacket) header).getStatus();
                return status == HttpStatus.CREATED_201.getStatusCode();
            }
        });

    }

    /**
     * Store a relationship, with an associated JSON property object,
     * between two objects in the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * JsonNode properties = new ObjectMapper().readTree("{ \"foo\" : \"bar\" }");
     * RelationMetadata result =
     *         client.relation("someCollection", "someKey")
     *               .to("anotherCollection", "anotherKey")
     *               .put(kind, properties)
     *               .get();
     * }
     * </pre>
     *
     * @param kind The name of the relationship to create.
     * @param properties A json object representing the properties of this relationship.
     * @return A prepared create request.
     */
    public OrchestrateRequest<RelationMetadata> put(final String kind, final JsonNode properties) {
        HttpContent packet = prepareCreateRelation(kind, properties);
        return new OrchestrateRequest<RelationMetadata>(client, packet, new ResponseConverter<RelationMetadata>() {
            @Override
            public RelationMetadata from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();
                final int status = ((HttpResponsePacket) header).getStatus();

                if (status == HttpStatus.CREATED_201.getStatusCode()) {
                    final String ref = header.getHeader(Header.ETag)
                            .replace("\"", "")
                            .replace("-gzip", "");
                    return new Relation(
                        sourceCollection, sourceKey,
                        destCollection, destKey,
                        kind, ref, properties
                    );
                }
                return null;
            }
        });
    }

    private HttpContent prepareCreateRelation(final String kind, final JsonNode properties) {

        checkNotNullOrEmpty(kind, "kind");
        checkArgument(destCollection != null && destKey != null,
                "'destCollection' and 'destKey' required for PUT query.");

        String localSourceCollection = invert ? destCollection : sourceCollection;
        String localSourceKey = invert ? destKey : sourceKey;
        String localDestCollection = invert ? sourceCollection : destCollection;
        String localDestKey = invert ? sourceKey : destKey;

        final String uri = client.uri(
                localSourceCollection, localSourceKey, "relation", kind,
                localDestCollection, localDestKey);

        HttpRequestPacket.Builder requestBuilder = HttpRequestPacket.builder()
            .method(Method.PUT)
            .contentType("application/json")
            .uri(uri);

        if (objectRef != null) {
            requestBuilder.header(Header.IfMatch, "\"".concat(objectRef).concat("\""));
        } else if (ifAbsent) {
            requestBuilder.header(Header.IfNoneMatch, "\"*\"");
        }

        byte[] content = null;
        if (properties != null) {
            content = toJsonBytes(properties);
            requestBuilder.contentLength(content.length);
        }

        HttpRequestPacket request = requestBuilder.build();

        HttpContent.Builder<?> httpContentBuilder = request.httpContentBuilder();
        if (properties != null) {
            httpContentBuilder.content(new ByteBufferWrapper(ByteBuffer.wrap(content)));
        }

        HttpContent packet = httpContentBuilder.build();
        return packet;
    }

    /**
     * Remove a relationship between two objects in the Orchestrate service.
     *
     * <pre>
     * {@code
     * boolean result =
     *         client.relation("someCollection", "someKey")
     *               .to("anotherCollection", "anotherKey")
     *               .purge(kind)
     *               .get();
     * }
     * </pre>
     *
     * @param kind The name of the relationship to delete.
     * @return A prepared delete request.
     */
    public OrchestrateRequest<Boolean> purge(final String kind) {
        checkNotNullOrEmpty(kind, "kind");
        checkArgument(destCollection != null && destKey != null,
                "'destCollection' and 'destKey' required for DELETE query.");

        String localSourceCollection = invert ? destCollection : sourceCollection;
        String localSourceKey = invert ? destKey : sourceKey;
        String localDestCollection = invert ? sourceCollection : destCollection;
        String localDestKey = invert ? sourceKey : destKey;

        final String uri = client.uri(
                localSourceCollection, localSourceKey, "relation", kind,
                localDestCollection, localDestKey);

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.DELETE)
                .uri(uri)
                .query("purge=true")
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<Boolean>(client, packet, new ResponseConverter<Boolean>() {
            @Override
            public Boolean from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                return (status == HttpStatus.NO_CONTENT_204.getStatusCode());
            }
        });
    }

    /**
     * The "destination" object to point the relationship to.
     *
     * @param collection The collection containing the destination key.
     * @param key The destination key to add the relation to.
     * @return This relation resource.
     */
    public RelationResource to(
            final String collection, final String key) {
        this.destCollection = checkNotNullOrEmpty(collection, "collection");
        this.destKey = checkNotNullOrEmpty(key, "key");

        return this;
    }

    /**
     * Swap the "source" and "destination" in the request, equivalent to:
     *
     * <pre>
     * {@code
     * this.invert(Boolean.TRUE);
     * }
     * </pre>
     *
     * @return This relation resource.
     * @see #invert(boolean)
     */
    public RelationResource invert() {
        return invert(Boolean.TRUE);
    }

    /**
     * If {@code invert} is {@code true} the "source" and "destination" objects
     * will be swapped in the request. This is useful for designing a
     * bi-directional relationship.
     *
     * @param invert Whether to invert the request.
     * @return This relation resource.
     */
    public RelationResource invert(final boolean invert) {
        this.invert = invert;
        return this;
    }

    /**
     * The number of relation results to get in this query, this value cannot
     * exceed 100. This property is ignored in {@code #put(...)} and {@code
     * #purge(...)} requests.
     *
     * @param limit The number of search results in this query.
     * @return This request.
     */
    public RelationResource limit(final int limit) {
        this.limit = checkNotNegative(limit, "limit");
        return this;
    }

    /**
     * The position in the results list to start retrieving results from,
     * this is useful for paginating results. This property is ignored in {@code
     * #put(...)} and {@code #purge(...)} requests.
     *
     * @param offset The position to start retrieving results from.
     * @return This request.
     */
    public RelationResource offset(final int offset) {
        this.offset = checkNotNegative(offset, "offset");
        return this;
    }

}
