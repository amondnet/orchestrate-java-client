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

import io.orchestrate.client.jsonpatch.JsonPatch;
import lombok.NonNull;
import org.glassfish.grizzly.http.*;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.memory.ByteBufferWrapper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;

import static io.orchestrate.client.Preconditions.checkArgument;

/**
 * The resource for the KV features in the Orchestrate API.
 */
public class KvResource extends BaseResource {

    /** The collection for the request. */
    private final String collection;
    /** The key for the request. */
    private final String key;
    /** Whether to store the object if no key already exists. */
    private boolean ifAbsent;
    /** The last known version of the stored object. */
    private String objectRef;

    KvResource(final OrchestrateClient client,
               final JacksonMapper mapper,
               final String collection,
               final String key) {
        super(client, mapper);
        assert (collection != null);
        assert (collection.length() > 0);
        assert (key != null);
        assert (key.length() > 0);

        this.collection = collection;
        this.key = key;
        this.ifAbsent = false;
        this.objectRef = null;
    }

    /**
     * {@link #delete(boolean)}.
     * @return The prepared delete request.
     */
    public OrchestrateRequest<Boolean> delete() {
        return delete(Boolean.FALSE);
    }

    /**
     * Purge a KV object from a collection in the Orchestrate.io service. This
     * will permanently remove the record from the service, and cannot be
     * undone.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * boolean result =
     *         client.kv("someCollection", "someKey")
     *               .delete(Boolean.TRUE)
     *               .get();
     * }
     * </pre>
     *
     * @param purge If {@code true}, permanently delete the object AND its "ref"
     *              history from Orchestrate.
     * @return The prepared delete request.
     */
    public OrchestrateRequest<Boolean> delete(final boolean purge) {
        checkArgument(!ifAbsent, "'ifAbsent' cannot be used in a DELETE request.");

        final String uri = client.uri(collection, key);

        final HttpRequestPacket.Builder builder = HttpRequestPacket.builder()
                .method(Method.DELETE)
                .uri(uri);
        if (purge) {
            builder.query("purge=true");
        }
        if (objectRef != null) {
            builder.header(Header.IfMatch, "\"".concat(objectRef).concat("\""));
        }

        final HttpContent packet = builder.build().httpContentBuilder().build();
        return new OrchestrateRequest<Boolean>(client, packet, new ResponseConverter<Boolean>() {
            @Override
            public Boolean from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                return (status == HttpStatus.NO_CONTENT_204.getStatusCode());
            }
        });
    }

    /**
     * {@link #get(Class)}.
     * @param clazz Type information for marshalling objects at runtime.
     * @param <T> The type to deserialize the result to.
     * @return This KV resource.
     */
    public <T> OrchestrateRequest<KvObject<T>> get(final Class<T> clazz) {
        return get(clazz, null);
    }

    /**
     * Fetch an object by key from the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * KvObject<DomainObject> object =
     *         client.kv("someCollection", "someKey")
     *               .get(DomainObject.class)
     *               .get();
     * }
     * </pre>
     *
     * @param clazz Type information for marshalling objects at runtime.
     * @param ref The version of the object to get.
     * @param <T> The type to deserialize the result to.
     * @return This KV resource.
     */
    public <T> OrchestrateRequest<KvObject<T>> get(final @NonNull Class<T> clazz, @Nullable final String ref) {
        final String uri = ref != null ?
                client.uri(collection, key, "refs", ref) :
                client.uri(collection, key);

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.GET)
                .uri(uri)
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<KvObject<T>>(client, packet, new ResponseConverter<KvObject<T>>() {
            @Override
            public KvObject<T> from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();
                final int status = ((HttpResponsePacket) header).getStatus();

                if (status == 404) {
                    // maybe one day we can return an optional type
                    return null;
                }

                return toKvObject(response, collection, key, clazz);
            }
        });
    }

    /**
     * Equivalent to {@code this.ifAbsent(Boolean.TRUE)}.
     *
     * @return This KV resource.
     * @see #ifAbsent(boolean)
     */
    public KvResource ifAbsent() {
        return ifAbsent(Boolean.TRUE);
    }

    /**
     * Whether to store the object if no key already exists.
     *
     * @param ifAbsent If {@code true}
     * @return This KV resource.
     */
    public KvResource ifAbsent(final boolean ifAbsent) {
        checkArgument(!ifAbsent || objectRef == null, "'ifMatch' and 'ifAbsent' cannot be used together.");

        this.ifAbsent = ifAbsent;
        return this;
    }

    /**
     * The last known version of the stored object to match for the request to
     * succeed.
     *
     * @param objectRef The last known version of the stored object.
     * @return This KV resource.
     */
    public KvResource ifMatch(final @NonNull String objectRef) {
        checkArgument(!ifAbsent, "'ifMatch' and 'ifAbsent' cannot be used together.");

        this.objectRef = objectRef;
        return this;
    }

    /**
     * Store an object by key to the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * DomainObject obj = new DomainObject();
     * KvMetadata kvMetadata =
     *         client.kv("someCollection", "someKey")
     *               .put(obj)
     *               .get();
     * }
     * </pre>
     *
     * @param value The object to store.
     * @return The prepared put request.
     */
    public OrchestrateRequest<KvMetadata> put(final @NonNull Object value) {
        final byte[] content = toJsonBytes(value);

        final String uri = client.uri(collection, key);

        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.PUT)
                .contentType("application/json")
                .uri(uri);
        if (objectRef != null) {
            httpHeaderBuilder.header(Header.IfMatch, "\"".concat(objectRef).concat("\""));
        } else if (ifAbsent) {
            httpHeaderBuilder.header(Header.IfNoneMatch, "\"*\"");
        }
        httpHeaderBuilder.contentLength(content.length);

        final HttpContent packet = httpHeaderBuilder.build()
                .httpContentBuilder()
                .content(new ByteBufferWrapper(ByteBuffer.wrap(content)))
                .build();
        return new OrchestrateRequest<KvMetadata>(client, packet, new ResponseConverter<KvMetadata>() {
            @Override
            public KvMetadata from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();
                final int status = ((HttpResponsePacket) header).getStatus();

                if (status == 201) {
                    final String ref = header.getHeader(Header.ETag)
                            .replace("\"", "")
                            .replace("-gzip", "");
                    return new KvMetadata(collection, key, ref);
                }
                return null;
            }
        });
    }

    public OrchestrateRequest<KvMetadata> patch(JsonPatch patchOps) {
        final byte[] content = toJsonBytes(patchOps.getOps());

        final String uri = client.uri(collection, key);

        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.PATCH)
                .contentType("application/json-patch+json")
                .uri(uri);
        if (objectRef != null) {
            httpHeaderBuilder.header(Header.IfMatch, "\"".concat(objectRef).concat("\""));
        } else if (ifAbsent) {
            throw new IllegalStateException("Cannot perform an ifAbsent PATCH request.");
        }
        httpHeaderBuilder.contentLength(content.length);

        final HttpContent packet = httpHeaderBuilder.build()
                .httpContentBuilder()
                .content(new ByteBufferWrapper(ByteBuffer.wrap(content)))
                .build();
        return new OrchestrateRequest<KvMetadata>(client, packet, new ResponseConverter<KvMetadata>() {
            @Override
            public KvMetadata from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();
                final int status = ((HttpResponsePacket) header).getStatus();

                if (status == 201) {
                    final String ref = header.getHeader(Header.ETag)
                            .replace("\"", "")
                            .replace("-gzip", "");
                    return new KvMetadata(collection, key, ref);
                }
                return null;
            }
        });
    }

    public OrchestrateRequest<KvMetadata> merge(String jsonObject) {
        final byte[] content = toJsonBytes(jsonObject);

        final String uri = client.uri(collection, key);

        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.PATCH)
                .contentType("application/merge-patch+json")
                .uri(uri);
        if (objectRef != null) {
            httpHeaderBuilder.header(Header.IfMatch, "\"".concat(objectRef).concat("\""));
        } else if (ifAbsent) {
            throw new IllegalStateException("Cannot perform an ifAbsent PATCH request.");
        }
        httpHeaderBuilder.contentLength(content.length);

        final HttpContent packet = httpHeaderBuilder.build()
                .httpContentBuilder()
                .content(new ByteBufferWrapper(ByteBuffer.wrap(content)))
                .build();
        return new OrchestrateRequest<KvMetadata>(client, packet, new ResponseConverter<KvMetadata>() {
            @Override
            public KvMetadata from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();
                final int status = ((HttpResponsePacket) header).getStatus();

                if (status == 201) {
                    final String ref = header.getHeader(Header.ETag)
                            .replace("\"", "")
                            .replace("-gzip", "");
                    return new KvMetadata(collection, key, ref);
                }
                return null;
            }
        });
    }
}
