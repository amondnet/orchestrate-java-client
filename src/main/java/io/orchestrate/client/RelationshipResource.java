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
 * The resource for the relationship features in the Orchestrate API.
 */
public class RelationshipResource extends BaseResource {

    /** The collection containing the source key. */
    private String sourceCollection;
    /** The source key to add the relationship to. */
    private String sourceKey;
    /** The collection containing the destination key. */
    private String destCollection;
    /** The destination key to add the relationship to. */
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
    /** The fully-qualified names of fields to select when filtering the result JSON */
    private String withFields;
    /** The fully-qualified names of fields to reject when filtering the result JSON */
    private String withoutFields;

    RelationshipResource(final OrchestrateClient client,
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
        this.withFields = null;
        this.withoutFields = null;
    }

    /**
     * Store a relationship between two objects in the Orchestrate service as part of a bulk operation.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * client.bulk()
     *   .add(client.relationship("someCollection", "key1").to("someCollection", "key2").bulkPut("someRelationship"))
     *   .add(...)
     *   .done();
     * }
     * </pre>
     * @param relation The name of the relationship.
     * @return The bulk operation.
     *
     * @see #put(String)
     * @see Client#bulk()
     */
    public BulkOperation bulkPut(final String relation) {
        return BulkOperation.forRelationship(sourceCollection, sourceKey, destCollection, destKey, relation, null);
    }

    /**
     * Store a relationship between two objects in the Orchestrate service as part of a bulk operation.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * client.bulk()
     *   .add(client.relationship("someCollection", "key1").to("someCollection", "key2").bulkPut("someRelationship", obj))
     *   .add(...)
     *   .done();
     * }
     * </pre>
     * @param relation The name of the relationship.
     * @param properties A json object representing the properties of this relationship.
     * @return the bulk operation.
     *
     * @see #put(String, Object)
     * @see Client#bulk()
     */
    public BulkOperation bulkPut(final String relation, final Object properties) {
        return BulkOperation.forRelationship(sourceCollection, sourceKey, destCollection, destKey, relation, properties);
    }

    /**
     * Equivalent to {@code this.ifAbsent(Boolean.TRUE)}.
     *
     * @return This RelationshipResource.
     * @see #ifAbsent(boolean)
     */
    public RelationshipResource ifAbsent() {
        return ifAbsent(Boolean.TRUE);
    }

    /**
     * Whether to store the object if no such relationship already exists.
     *
     * @param ifAbsent If {@code true}
     * @return This RelationshipResource.
     */
    public RelationshipResource ifAbsent(final boolean ifAbsent) {
        checkArgument(!ifAbsent || objectRef == null, "'ifMatch' and 'ifAbsent' cannot be used together.");

        this.ifAbsent = ifAbsent;
        return this;
    }

    /**
     * The last known version of the stored object to match for the request to
     * succeed.
     *
     * @param objectRef The last known version of the stored object.
     * @return This RelationshipResource.
     */
    public RelationshipResource ifMatch(final @NonNull String objectRef) {
        checkArgument(!ifAbsent, "'ifMatch' and 'ifAbsent' cannot be used together.");

        this.objectRef = objectRef;
        return this;
    }

    /**
     * Retrieve a Relationship object.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * Relationship relationship =
     *         client.relationship("someCollection", "someKey")
     *               .get("someRelation", "otherCollection", "otherKey")
     *               .get();
     * }
     * </pre>
     *
     * @param relation String The name of the relationship.
     * @param destCollection String The name of destination collection.
     * @param destKey String The name of destination key.
     * @return A prepared get request.
     */
    public <T> OrchestrateRequest<Relationship<T>> get(final Class<T> clazz, final String relation, final String destCollection, final String destKey) {

        checkNotNullOrEmpty(relation, "relation");
        checkNotNullOrEmpty(destCollection, "destCollection");
        checkNotNullOrEmpty(destKey, "destKey");

        final String uri = client.uri(sourceCollection, sourceKey, "relation", relation, destCollection, destKey);
        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.GET)
                .uri(uri);

        String query = "";
        if (withFields != null) {
            query = query.concat("&with_fields=").concat(client.encode(withFields));
        }
        if (withoutFields != null) {
            query = query.concat("&without_fields=").concat(client.encode(withoutFields));
        }
        httpHeaderBuilder.query(query);

        final HttpContent packet = httpHeaderBuilder
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<Relationship<T>>(client, packet, new ResponseConverter<Relationship<T>>() {
            @Override
            public Relationship<T> from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();
                final int status = ((HttpResponsePacket) header).getStatus();
                assert (status == 200 || status == 404);

                if (status == 404) {
                    return null;
                }

                final String ref = header.getHeader(Header.ETag)
                        .replace("\"", "")
                        .replace("-gzip", "");

                JsonNode valueNode = toJsonNodeOrNull(response);

                final T value = ResponseConverterUtil.jsonToDomainObject(mapper, valueNode, clazz);
                String rawValue = null;
                if(value != null && value instanceof String) {
                    rawValue = (String)value;
                }

                // The server doesn't send reftime on graph relationship GET
                Long reftime = null;

                return new Relationship<T>(
                    mapper,
                    sourceCollection, sourceKey,
                    relation, destCollection, destKey,
                    ref, reftime,
                    value, valueNode, rawValue
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
     * RelationshipList<String> relatedObjects =
     *         client.relationship("someCollection", "someKey")
     *               .get(String.class, "someRelation")
     *               .get();
     * }
     * </pre>
     *
     * @param clazz Type information for deserializing to type {@code T} at
     *              runtime.
     * @param relations The name of the relationships to traverse to the related
     *              objects.
     * @param <T> The type to deserialize the response from the request to.
     * @return A prepared get request.
     */
    public <T> OrchestrateRequest<RelationshipList<T>> get(final Class<T> clazz, final String... relations) {
        checkNotNull(clazz, "clazz");
        checkArgument(destCollection == null && destKey == null,
                "'destCollection' and 'destKey' not valid in GET query.");
        checkNoneEmpty(relations, "relations", "relation");

        final String uri = client.uri(sourceCollection, sourceKey, "relations").concat("/" + client.encode(relations));

        String query = "limit=".concat(limit + "")
                .concat("&offset=").concat(offset + "");

        if (withFields != null) {
            query = query.concat("&with_fields=").concat(client.encode(withFields));
        }
        if (withoutFields != null) {
            query = query.concat("&without_fields=").concat(client.encode(withoutFields));
        }

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.GET)
                .uri(uri)
                .query(query)
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<RelationshipList<T>>(client, packet, new ResponseConverter<RelationshipList<T>>() {
            @Override
            public RelationshipList<T> from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                assert (status == 200 || status == 404);

                if (status == 404) {
                    return null;
                }

                final JsonNode jsonNode = toJsonNode(response);

                OrchestrateRequest<RelationshipList<T>> next = parseLink("next", jsonNode, this);
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
                    next = new OrchestrateRequest<RelationshipList<T>>(client, packet, this, false);
                } else {
                    next = null;
                }

                final int count = jsonNode.path("count").asInt();
                final List<KvObject<T>> relatedObjects = new ArrayList<KvObject<T>>(count);

                for (JsonNode node : jsonNode.path("results")) {
                    relatedObjects.add(toKvObject(node, clazz));
                }

                return new RelationshipList<T>(relatedObjects, next);
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
     *         client.relationship("someCollection", "someKey")
     *               .to("anotherCollection", "anotherKey")
     *               .put(relation)
     *               .get();
     * }
     * </pre>
     *
     * @param relation The name of the relationship to create.
     * @return A prepared put request.
     */
    public OrchestrateRequest<Boolean> put(final String relation) {
        HttpContent packet = prepareCreateRelationship(relation, null);
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
     *         client.relationship("someCollection", "someKey")
     *               .to("anotherCollection", "anotherKey")
     *               .put(relation, properties)
     *               .get();
     * }
     * </pre>
     *
     * @param relation The name of the relationship to create.
     * @param properties A json object representing the properties of this relationship.
     * @return A prepared create request.
     */
    public OrchestrateRequest<RelationshipMetadata> put(final String relation, final Object properties) {
        HttpContent packet = prepareCreateRelationship(relation, properties);
        return new OrchestrateRequest<RelationshipMetadata>(client, packet, new ResponseConverter<RelationshipMetadata>() {
            @Override
            public RelationshipMetadata from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();
                final int status = ((HttpResponsePacket) header).getStatus();

                if (status == HttpStatus.CREATED_201.getStatusCode()) {
                    final String ref = header.getHeader(Header.ETag)
                            .replace("\"", "")
                            .replace("-gzip", "");

                    // The server doesn't send reftime on graph relationship PUT
                    Long reftime = null;

                    return new Relationship<Object>(
                        mapper,
                        sourceCollection, sourceKey,
                        relation,
                        destCollection, destKey,
                        ref, reftime,
                        (Object) null, (JsonNode) null, (String) null
                    );
                }
                return null;
            }
        });
    }

    private HttpContent prepareCreateRelationship(final String relation, final Object properties) {

        checkNotNullOrEmpty(relation, "relation");
        checkArgument(destCollection != null && destKey != null,
                "'destCollection' and 'destKey' required for PUT query.");

        String localSourceCollection = invert ? destCollection : sourceCollection;
        String localSourceKey = invert ? destKey : sourceKey;
        String localDestCollection = invert ? sourceCollection : destCollection;
        String localDestKey = invert ? sourceKey : destKey;

        final String uri = client.uri(
                localSourceCollection, localSourceKey, "relation", relation,
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
     *         client.relationship("someCollection", "someKey")
     *               .to("anotherCollection", "anotherKey")
     *               .purge(relation)
     *               .get();
     * }
     * </pre>
     *
     * @param relation The name of the relationship to delete.
     * @return A prepared delete request.
     */
    public OrchestrateRequest<Boolean> purge(final String relation) {
        checkNotNullOrEmpty(relation, "relation");
        checkArgument(destCollection != null && destKey != null,
                "'destCollection' and 'destKey' required for DELETE query.");

        String localSourceCollection = invert ? destCollection : sourceCollection;
        String localSourceKey = invert ? destKey : sourceKey;
        String localDestCollection = invert ? sourceCollection : destCollection;
        String localDestKey = invert ? sourceKey : destKey;

        final String uri = client.uri(
                localSourceCollection, localSourceKey, "relation", relation,
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
     * @param key The destination key to add the relationship to.
     * @return This relationship resource.
     */
    public RelationshipResource to(
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
     * @return This relationship resource.
     * @see #invert(boolean)
     */
    public RelationshipResource invert() {
        return invert(Boolean.TRUE);
    }

    /**
     * If {@code invert} is {@code true} the "source" and "destination" objects
     * will be swapped in the request. This is useful for designing a
     * bi-directional relationship.
     *
     * @param invert Whether to invert the request.
     * @return This relationship resource.
     */
    public RelationshipResource invert(final boolean invert) {
        this.invert = invert;
        return this;
    }

    /**
     * The number of relationship results to get in this query, this value cannot
     * exceed 100. This property is ignored in {@code #put(...)} and {@code
     * #purge(...)} requests.
     *
     * @param limit The number of search results in this query.
     * @return This request.
     */
    public RelationshipResource limit(final int limit) {
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
    public RelationshipResource offset(final int offset) {
        this.offset = checkNotNegative(offset, "offset");
        return this;
    }

    /**
     * Apply field-filtering to the result JSON, using this list of fully-qualified
     * field names as a whitelist of fields to include.
     *
     * <p>
     * {@code
     * Relationship relationship =
     *         client.relationship("someCollection", "someKey")
     *               .get("someRelation", "otherCollection", "otherKey")
     *               .withFields("value.name.first,value.name.last")
     *               .get();
     * }
     * </p>
     *
     * @param withFields The comma separated list of fully-qualified field names to select.
     * @return This request.
     */
    public RelationshipResource withFields(final String withFields) {
        this.withFields = checkNotNull(withFields, "withFields");
        return this;
    }

    /**
     * Apply field-filtering to the result JSON, using this list of fully-qualified
     * field names as a blacklist of fields to exclude.
     *
     * <p>
     * {@code
     * Relationship relationship =
     *         client.relationship("someCollection", "someKey")
     *               .get("someRelation", "otherCollection", "otherKey")
     *               .withoutFields("value.name.first,value.name.last")
     *               .get();
     * }
     * </p>
     *
     * @param withoutFields The comma separated list of fully-qualified field names to reject.
     * @return This request.
     */
    public RelationshipResource withoutFields(final String withoutFields) {
        this.withoutFields = checkNotNull(withoutFields, "withoutFields");
        return this;
    }

}
