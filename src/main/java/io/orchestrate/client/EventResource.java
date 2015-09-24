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
import lombok.NonNull;
import org.glassfish.grizzly.http.*;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.memory.ByteBufferWrapper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static io.orchestrate.client.Preconditions.*;

/**
 * The resource for the event features in the Orchestrate API.
 */
public class EventResource extends BaseResource {

    /** The collection for the request. */
    private final String collection;
    /** The key for the request. */
    private final String key;
    /** The type of events to get. */
    private String type;
    /** The timestamp to get events from. */
    private Long start;
    /** The timestamp to get events up to. */
    private Long end;
    /** The number of KV objects to retrieve. */
    private int limit;

    private Long timestamp;

    EventResource(final OrchestrateClient client,
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
        this.start = null;
        this.end = null;
        this.limit = 10;
    }

    /**
     * Fetch events for a key in the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * EventList<DomainObject> events =
     *         client.event("someCollection", "someKey")
     *               .type("eventType")
     *               .get(DomainObject.class)
     *               .get();
     * }
     * </pre>
     *
     * @param clazz Type information for marshalling objects at runtime.
     * @param <T> The type to deserialize the result of the request to.
     * @return The prepared get request.
     */
    public <T> OrchestrateRequest<EventList<T>> get(final Class<T> clazz) {
        checkNotNull(clazz, "clazz");
        checkNotNull(type, "type");

        final String uri = client.uri(collection, key, "events", type);
        String query = "limit=".concat(Integer.toString(limit));
        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.GET)
                .uri(uri);
        if (start != null) {
            query += "&start=" + start;
        }
        if (end != null) {
            query += "&end=" + end;
        }
        httpHeaderBuilder.query(query);

        final HttpContent packet = httpHeaderBuilder.build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<EventList<T>>(client, packet, new ResponseConverter<EventList<T>>() {
            @Override
            public EventList<T> from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                assert (status == 200);

                final JsonNode jsonNode = toJsonNode(response);

                final int count = jsonNode.get("count").asInt();
                final List<Event<T>> events = new ArrayList<Event<T>>(count);

                final Iterator<JsonNode> iter = jsonNode.get("results").elements();
                while (iter.hasNext()) {
                    final JsonNode result = iter.next();
                    events.add(ResponseConverterUtil.wrapperJsonToEvent(mapper, result, clazz));
                }
                return new EventList<T>(events);
            }

        });
    }

    /**
     * {@link #put(Object, Long)}.
     * @deprecated Use {@link #create(Object)} for adding new events, and SingleEventResource's 'update'
     *  (via {@link #ordinal(String)}) for updating existing event instances.
     * @param value The value for the event.
     * @return The prepared put request.
     */
    public OrchestrateRequest<Boolean> put(final @NonNull Object value) {
        // Does NOT refer to current 'timestamp' because that is not how legacy worked (pre-deprecation).
        return put(value, null);
    }

    /**
     * Store an event to a key in the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * DomainObject obj = new DomainObject(); // a POJO
     * boolean result =
     *         client.event("someCollection", "someKey")
     *               .type("someType")
     *               .put(obj)
     *               .get();
     * }
     * </pre>
     *
     * @param value The object to store as the event.
     * @param timestamp The timestamp to store the event at.
     * @return The prepared put request.
     * @deprecated Use {@link #create(Object)} for adding new events, and SingleEventResource's 'update'
     *  (via {@link #ordinal(String)}) for updating existing event instances.
     */
    public OrchestrateRequest<Boolean> put(
            final @NonNull Object value, @Nullable final Long timestamp) {
        checkNotNull(type, "type");
        checkArgument(start == null && end == null, "'start' and 'end' not allowed with PUT requests.");

        final byte[] content = toJsonBytes(value);

        final String uri = client.uri(collection, key, "events", type);

        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.PUT)
                .contentType("application/json")
                .uri(uri);
        if (timestamp != null) {
            httpHeaderBuilder.query("timestamp=" + timestamp);
        }
        httpHeaderBuilder.contentLength(content.length);

        final HttpContent packet = httpHeaderBuilder.build()
                .httpContentBuilder()
                .content(new ByteBufferWrapper(ByteBuffer.wrap(content)))
                .build();

        return new OrchestrateRequest<Boolean>(client, packet, new ResponseConverter<Boolean>() {
            @Override
            public Boolean from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                return (status == 204);
            }
        });
    }

    /**
     * Add an event to a key in the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * DomainObject obj = new DomainObject(); // a POJO
     * EventMetadata eventMeta =
     *         client.event("someCollection", "someKey")
     *               .type("someType")
     *               .create(obj)
     *               .get();
     * }
     * </pre>
     *
     * <pre>
     * To add an event to a specific timestamp:
     * {@code
     * DomainObject obj = new DomainObject(); // a POJO
     * EventMetadata eventMeta =
     *         client.event("someCollection", "someKey")
     *               .type("someType")
     *               .timestamp(someTimestamp)
     *               .create(obj)
     *               .get();
     * }
     * </pre>
     * @param value The object to store as the event.
     * @return The prepared create request.
     */
    public OrchestrateRequest<EventMetadata> create(final @NonNull Object value) {
        checkNotNull(type, "type");
        checkArgument(start == null && end == null, "'start' and 'end' not allowed with 'create' requests.");

        final byte[] content = toJsonBytes(value);

        final String uri;
        if(timestamp != null) {
            uri = client.uri(collection, key, "events", type, ""+timestamp);
        } else {
            uri = client.uri(collection, key, "events", type);
        }

        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.POST)
                .contentType("application/json")
                .uri(uri);
        httpHeaderBuilder.contentLength(content.length);

        final HttpContent packet = httpHeaderBuilder.build()
                .httpContentBuilder()
                .content(new ByteBufferWrapper(ByteBuffer.wrap(content)))
                .build();

        return new OrchestrateRequest<EventMetadata>(client, packet, new ResponseConverter<EventMetadata>() {
            @Override
            public EventMetadata from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();

                final int status = ((HttpResponsePacket) header).getStatus();

                if (status == 201) {
                    final String location = header.getHeader(Header.Location);
                    //  /v0/{coll}/{key}/events/{type}/{timestamp}/{ordinal}
                    final String[] parts = location.split("/");
                    Long timestamp = new Long(parts[6]);
                    String ordinal = parts[7];

                    final String ref = header.getHeader(Header.ETag)
                            .replace("\"", "")
                            .replace("-gzip", "");
                    return new Event<Void>(mapper, collection, key, type, timestamp, ordinal, ref, null, null, null);
                }
                return null;
            }
        });
    }

    /**
     * The type for an event, e.g. "update" or "tweet" etc.
     *
     * @param type The type of the event.
     * @return The event resource.
     */
    public EventResource type(final String type) {
        this.type = checkNotNullOrEmpty(type, "type");

        return this;
    }

    public EventResource limit(final int limit) {
        this.limit = checkNotNegative(limit, "limit");

        return this;
    }

    /**
     * The timestamp for an event.
     *
     * @param timestamp The timestamp of the event.
     * @return The event resource.
     */
    public EventResource timestamp(final Long timestamp) {
        this.timestamp = timestamp;

        return this;
    }

    /**
     * Called to make operations for an individual event instance. {@link #type}
     * and {@link #timestamp} must be called prior to calling this method.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * // fetches a single event instance
     * Event&lt;String&gt; event =
     *         client.event("someCollection", "someKey")
     *               .type("someType")
     *               .timestamp(someTimestamp)
     *               .ordinal(someOrdinal)
     *               .get(String.class)
     *               .get();
     * }
     * </pre>
     *
     * @param ordinal The event instance's ordinal value.
     * @return A SingleEventResource that can be used to perform operations for
     *   the individual event instance.
     */
    public SingleEventResource ordinal(final String ordinal) {
        checkNotNull(type, "type");
        checkNotNull(timestamp, "timestamp");
        checkNotNullOrEmpty(ordinal, "ordinal");
        return new SingleEventResource(client, jacksonMapper,
                collection, key, type, timestamp, ordinal);
    }

    /**
     * The inclusive start of a time range to query.
     *
     * @param start The timestamp to get events from.
     * @return The event resource.
     */
    public EventResource start(final long start) {
        this.start = start;
        return this;
    }

    /**
     * The exclusive end of a time range to query.
     *
     * @param end The timestamp to get events up to.
     * @return The event resource.
     */
    public EventResource end(final long end) {
        this.end = end;
        return this;
    }

}
