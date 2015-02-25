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
import io.orchestrate.client.jsonpatch.JsonPatch;
import lombok.NonNull;
import org.glassfish.grizzly.http.*;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.memory.ByteBufferWrapper;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The resource for interacting with an individual Event instance.
 */
public class SingleEventResource extends BaseResource {
    private final String collection;
    private final String key;
    private final String eventType;
    private final Long timestamp;
    private final String ordinal;
    private String ifMatchRef;

    SingleEventResource(final OrchestrateClient client,
                        final JacksonMapper mapper,
                        final String collection,
                        final String key,
                        final String eventType,
                        final Long timestamp,
                        final String ordinal) {
        super(client, mapper);
        assert (collection != null);
        assert (collection.length() > 0);
        assert (key != null);
        assert (key.length() > 0);
        assert (eventType != null);
        assert (eventType.length() > 0);
        assert (timestamp != null);
        assert (ordinal != null);

        this.collection = collection;
        this.key = key;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.ordinal = ordinal;
    }

    /**
     * Sets a conditional If-Match header. This is for conditional updates or deletes.
     * @param ref the expected ref
     * @return this resource
     */
    public SingleEventResource ifMatch(String ref) {
        this.ifMatchRef = ref;
        return this;
    }

    /**
     * Fetch the single Event instance.
     * @param clazz The Class to serialize the Event's value as.
     * @param <T> The type for the Event value.
     * @return An OrchestrateRequest representing the active request.
     */
    public <T> OrchestrateRequest<Event<T>> get(final Class<T> clazz) {
        final String uri = buildUri();

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.GET)
                .uri(uri)
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<Event<T>>(client, packet, new ResponseConverter<Event<T>>() {
            @Override
            public Event<T> from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();
                final int status = ((HttpResponsePacket) header).getStatus();

                if (status == 404) {
                    // maybe one day we can return an optional type
                    return null;
                }

                String rawWrapperJson = response.getContent().toStringContent();
                final JsonNode jsonNode = mapper.readTree(rawWrapperJson);

                return ResponseConverterUtil.wrapperJsonToEvent(mapper, jsonNode, clazz);
            }
        });
    }

    private String buildUri() {
        return client.uri(collection, key, "events", eventType, ""+timestamp, ordinal);
    }

    /**
     * Delete the Event instance.
     * @return The active request.
     */
    public OrchestrateRequest<Boolean> purge() {
        final String uri = buildUri();

        HttpRequestPacket.Builder builder = HttpRequestPacket.builder()
            .method(Method.DELETE)
            .query("purge=true")
            .uri(uri);

        if(ifMatchRef != null) {
            builder.header(Header.IfMatch, "\"".concat(ifMatchRef).concat("\""));
        }

        final HttpContent packet = builder
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
     * Update the Event instance to have an entirely new value.
     * @param value the updated value for the Event instance.
     * @return the active request.
     */
    public OrchestrateRequest<EventMetadata> update(final @NonNull Object value) {
        final byte[] content = toJsonBytes(value);

        final String uri = buildUri();

        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.PUT)
                .contentType("application/json")
                .uri(uri)
                .contentLength(content.length);

        if(ifMatchRef != null) {
            httpHeaderBuilder.header(Header.IfMatch, "\"".concat(ifMatchRef).concat("\""));
        }

        final HttpContent packet = httpHeaderBuilder.build()
                .httpContentBuilder()
                .content(new ByteBufferWrapper(ByteBuffer.wrap(content)))
                .build();

        return new OrchestrateRequest<EventMetadata>(client, packet, new EventMetadataResponseConverter());
    }

    /**
     * Patch (Partial Update) the Event.
     * @param patchOps The list of operations to perform in the patch.
     * @return the active request.
     */
    public OrchestrateRequest<EventMetadata> patch(JsonPatch patchOps) {
        final byte[] content = toJsonBytes(patchOps.getOps());

        final String uri = buildUri();

        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.PATCH)
                .contentType("application/json-patch+json")
                .uri(uri)
                .contentLength(content.length);

        if(ifMatchRef != null) {
            httpHeaderBuilder.header(Header.IfMatch, "\"".concat(ifMatchRef).concat("\""));
        }

        final HttpContent packet = httpHeaderBuilder.build()
                .httpContentBuilder()
                .content(new ByteBufferWrapper(ByteBuffer.wrap(content)))
                .build();

        return new OrchestrateRequest<EventMetadata>(client, packet, new EventMetadataResponseConverter());
    }

    /**
     * Merge patch the Event (Merge the json value with the specified json value).
     * @param jsonString the json string to merge into the Event value.
     * @return the active request.
     */
    public OrchestrateRequest<EventMetadata> merge(String jsonString) {
        final byte[] content = toJsonBytes(jsonString);

        final String uri = buildUri();

        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.PATCH)
                .contentType("application/merge-patch+json")
                .uri(uri)
                .contentLength(content.length);

        if(ifMatchRef != null) {
            httpHeaderBuilder.header(Header.IfMatch, "\"".concat(ifMatchRef).concat("\""));
        }

        final HttpContent packet = httpHeaderBuilder.build()
                .httpContentBuilder()
                .content(new ByteBufferWrapper(ByteBuffer.wrap(content)))
                .build();

        return new OrchestrateRequest<EventMetadata>(client, packet, new EventMetadataResponseConverter());
    }

    class EventMetadataResponseConverter implements ResponseConverter<EventMetadata> {
        @Override
        public EventMetadata from(HttpContent response) throws IOException {
            final HttpHeader header = response.getHttpHeader();

            final int status = ((HttpResponsePacket) header).getStatus();

            if (status == 201 || status == 204) {
                final String location = header.getHeader(Header.Location);
                //  /v0/{coll}/{key}/events/{type}/{timestamp}/{ordinal}
                final String[] parts = location.split("/");
                final Long timestamp = new Long(parts[6]);
                final String ordinal = parts[7];

                final String ref = header.getHeader(Header.ETag)
                        .replace("\"", "")
                        .replace("-gzip", "");
                return new Event<Void>(mapper, collection, key, eventType, timestamp, ordinal, ref, null, null, null);
            }
            return null;
        }
    }
}
