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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.*;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.http.util.UEncoder;
import org.glassfish.grizzly.memory.ByteBufferWrapper;
import org.glassfish.grizzly.nio.NIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static io.orchestrate.client.Preconditions.*;

/**
 * The client used to read and write data to the Orchestrate service.
 */
@Slf4j
public class OrchestrateClient implements Client {

    /** The builder for this instance of the client. */
    private final Builder builder;
    /** The socket transport for HTTP messages. */
    private final NIOTransport transport;

    private static final ThreadLocal<UEncoder> ENCODER_HOLDER = new ThreadLocal<UEncoder>(){
        @Override
        protected UEncoder initialValue() {
            return new UEncoder();
        }
    };

    /**
     * Create a new {@code client} with the specified {@code apiKey} and default
     * {@code JacksonMapper}.
     *
     * <p>Equivalent to:
     * <pre>
     * {@code
     * Client client = OrchestrateClient.builder("your api key").build();
     * }
     * </pre>
     *
     * @param apiKey An API key for the Orchestrate.io service.
     */
    public OrchestrateClient(final String apiKey) {
        this(builder(apiKey));
    }

    /**
     * Create a new {@code client} with the specified {@code apiKey} and {@code
     * objectMapper}.
     *
     * @param apiKey An API key for the Orchestrate.io service.
     * @param objectMapper The Jackson JSON mapper to marshall data with.
     */
    public OrchestrateClient(final String apiKey, final ObjectMapper objectMapper) {
        this(new Builder(apiKey).mapper(objectMapper));
    }

    /**
     * Create a new {@code client} with the specified {@code apiKey} and {@code
     * mapper}.
     *
     * @param apiKey An API key for the Orchestrate.io service.
     * @param mapper The mapper to marshall data with.
     */
    public OrchestrateClient(final String apiKey, final JacksonMapper mapper) {
        this(new Builder(apiKey).mapper(mapper));
    }

    private OrchestrateClient(final Builder builder) {
        assert (builder != null);

        this.builder = builder;

        final ThreadPoolConfig poolConfig = ThreadPoolConfig.defaultConfig()
                .setPoolName("OrchestrateClientPool")
                .setCorePoolSize(builder.poolSize)
                .setMaxPoolSize(builder.maxPoolSize);
        // TODO add support for GZip compression
        final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless()
                .add(new TransportFilter());
        if (builder.useSSL) {
            final SSLEngineConfigurator serverConfig = initializeSSL();
            final SSLEngineConfigurator clientConfig = serverConfig.copy().setClientMode(true);

            filterChainBuilder.add(new SSLFilter(serverConfig, clientConfig));
        }

        HttpClientFilter httpClientFilter = new HttpClientFilter();
        httpClientFilter.addContentEncoding(new GZipContentEncoding());

        filterChainBuilder
                .add(httpClientFilter)
                .add(new ClientFilter(builder.apiKey, builder.host, builder.userAgent));
        // TODO experiment with the Leader-Follower IOStrategy
        this.transport = TCPNIOTransportBuilder.newInstance()
                .setTcpNoDelay(true)
                .setKeepAlive(true)
                .setWorkerThreadPoolConfig(poolConfig)
                .setIOStrategy(WorkerThreadIOStrategy.getInstance())
                .setProcessor(filterChainBuilder.build())
                .build();
    }

    private static SSLEngineConfigurator initializeSSL() {
        final SSLContextConfigurator sslContextConfig = new SSLContextConfigurator();
        return new SSLEngineConfigurator(sslContextConfig.createSSLContext(),
                false, false, false);
    }

//    <T> SafeFutureImpl<HttpContent> execute(
//            final HttpContent request,
//            final Set<ResponseCompletionHandler<T>> listeners) {
//        assert (request != null);
//        assert (listeners != null);
//
//        final SafeFutureImpl<HttpContent> future = SafeFutureImpl.create();
//
//        for (final ResponseCompletionHandler<T> listener : listeners) {
//            future.addCompletionHandler(listener);
//        }
//
//        try {
//            if (transport.isStopped()) {
//                transport.start();
//            }
//
//            final InetSocketAddress addr =
//                    new InetSocketAddress(builder.host.getHost(), builder.port);
//            transport.connect(addr, new ConnectionCompletionHandler(future, request));
//        } catch (final IOException e) {
//            future.failure(e);
//        }
//
//        return future;
//    }

    void execute(
            final HttpContent request,
            final ConnectionCompletionHandler handler) {
        assert (request != null);
        assert (handler != null);

        try {
            if (transport.isStopped()) {
                transport.start();
            }

            final InetSocketAddress addr =
                    new InetSocketAddress(builder.host.getHost(), builder.port);
            transport.connect(addr, handler);
        } catch (final IOException e) {
            handler.failed(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        if (transport != null && !transport.isStopped()) {
            transport.shutdownNow();
        }
    }

    /** {@inheritDoc} */
    @Override
    public OrchestrateRequest<Boolean> deleteCollection(final String collection) {
        checkNotNullOrEmpty(collection, "collection");

        final String uri = uri(collection);

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.DELETE)
                .uri(uri)
                .query("force=true")
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<Boolean>(this, packet, new ResponseConverter<Boolean>() {
            @Override
            public Boolean from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                return (status == HttpStatus.NO_CONTENT_204.getStatusCode());
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public EventResource event(final String collection, final String key) {
        checkNotNullOrEmpty(collection, "collection");
        checkNotNullOrEmpty(key, "key");

        return new EventResource(this, builder.mapper, collection, key);
    }

    /** {@inheritDoc} */
    @Override
    public KvResource kv(final String collection, final String key) {
        checkNotNullOrEmpty(collection, "collection");
        checkNotNullOrEmpty(key, "key");

        return new KvResource(this, builder.mapper, collection, key);
    }

    /** {@inheritDoc} */
    @Override
    public KvListResource listCollection(final String collection) {
        checkNotNullOrEmpty(collection, "collection");

        return new KvListResource(this, builder.mapper, collection);
    }

    /** {@inheritDoc} */
    @Override
    public void ping() throws IOException {
        final String uri = uri("");

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.HEAD)
                .uri(uri)
                .build()
                .httpContentBuilder()
                .build();

        new OrchestrateRequest<Void>(this, packet, new ResponseConverter<Void>() {
            @Override
            public Void from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                if (status != 200) {
                    throw new IOException(String.format("Ping failed: %s", status));
                }
                return null;
            }
        }).get(5000, TimeUnit.MILLISECONDS);
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public void ping(final String collection) throws IOException {
        this.ping();
    }

    /** {@inheritDoc} */
    @Override
    public OrchestrateRequest<KvMetadata> postValue(
            final String collection, final Object value) throws IOException {
        checkNotNullOrEmpty(collection, "collection");
        checkNotNull(value, "value");

        final byte[] content;
        try {
            content = (value instanceof String)
                    ? ((String) value).getBytes(Charset.forName("UTF-8"))
                    : builder.mapper.getMapper().writeValueAsBytes(value);
        } catch (final Exception e) {
            throw new RuntimeException(e); // FIXME
        }

        final String uri = this.uri(collection);

        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.POST)
                .contentType("application/json")
                .uri(uri)
                .contentLength(content.length);

        final HttpContent packet = httpHeaderBuilder.build()
                .httpContentBuilder()
                .content(new ByteBufferWrapper(ByteBuffer.wrap(content)))
                .build();
        return new OrchestrateRequest<KvMetadata>(this, packet, new ResponseConverter<KvMetadata>() {
            @Override
            public KvMetadata from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();
                final int status = ((HttpResponsePacket) header).getStatus();

                if (status == 201) {
                    final String key = header.getHeader(Header.Location).split("/")[3];
                    final String ref = header.getHeader(Header.ETag)
                            .replace("\"", "")
                            .replace("-gzip", "");
                    return new KvObject<Void>(collection, key, ref, null, builder.mapper.getMapper(), null, null, null);
                }
                return null;
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public RelationshipResource relationship(final String collection, final String key) {
        checkNotNullOrEmpty(collection, "collection");
        checkNotNullOrEmpty(key, "key");

        return new RelationshipResource(this, builder.mapper, collection, key);
    }

    /** {@inheritDoc} */
    @Override
    public CrossCollectionSearchResource search() {
        return new CrossCollectionSearchResource(this, builder.mapper);
    }

    /** {@inheritDoc} */
    @Override
    public CollectionSearchResource searchCollection(final String collection) {
        checkNotNullOrEmpty(collection, "collection");

        return new CollectionSearchResource(this, builder.mapper, collection);
    }

    /**
     * A new builder to create an {@code OrchestrateClient} with default
     * settings.
     *
     * @param apiKey An API key for the Orchestrate.io service.
     * @return A new {@code Builder} with default settings.
     */
    public static Builder builder(final String apiKey) {
        return new Builder(apiKey);
    }

    /**
     * Builder used to create {@code Client} instances.
     *
     * <p>Usage:
     * <pre>
     * {@code
     * Client client = OrchestrateClient.builder("your api key")
     *         .host("https://api.orchestrate.io")  // optional
     *         .port(80)           // optional
     *         .poolSize(Runtime.getRuntime().availableProcessors()) // optional
     *         .maxPoolSize(15)    // optional
     *         .build();
     * }
     * </pre>
     */
    public static final class Builder {

        /** The default host for the Orchestrate.io service. */
        public static final String DEFAULT_HOST = "https://api.orchestrate.io";
        /** The default port for the Orchestrate.io service. */
        public static final int DEFAULT_PORT = 443;

        /** An API key for the Orchestrate.io service. */
        private final String apiKey;
        /** The host for the Orchestrate.io service. */
        private URI host;
        /** The port for the Orchestrate.io service. */
        private int port;
        /** The number of threads to use with the client. */
        private int poolSize;
        /** The maximum size of the thread pool to use with the client. */
        private int maxPoolSize;
        /** The configured JSON mapper. */
        private JacksonMapper mapper;
        /** Whether to use SSL with the connection. */
        private boolean useSSL;
        /** Value to append as the "User-Agent" in requests to Orchestrate. */
        private String userAgent;

        private Builder(final String apiKey) {
            checkNotNullOrEmpty(apiKey, "apiKey");

            this.apiKey = apiKey;
            host(DEFAULT_HOST);
            port(DEFAULT_PORT);
            poolSize(Runtime.getRuntime().availableProcessors());
            maxPoolSize(Integer.MAX_VALUE);
            mapper(JacksonMapper.builder());
            useSSL(Boolean.TRUE);
        }

        /**
         * Set the hostname for the Orchestrate.io service, defaults to {@code
         * Builder.DEFAULT_HOST}.
         *
         * @param host The hostname for the Orchestrate.io service.
         * @return This builder.
         * @see Builder#DEFAULT_HOST
         */
        public Builder host(final String host) {
            checkNotNullOrEmpty(host, "host");

            this.host = URI.create(host);
            return this;
        }

        /**
         * Set the port for the Orchestrate.io service, defaults to {@code
         * Builder.DEFAULT_PORT}.
         *
         * @param port The port for the Orchestrate.io service.
         * @return This builder.
         * @see Builder#DEFAULT_PORT
         */
        public Builder port(final int port) {
            checkArgument(port > 0 && port <= 65535, "'port' must be between 1 and 65535.");

            this.port = port;
            return this;
        }

        /**
         * The initial number of threads to use with the client, defaults to
         * {@link Runtime#availableProcessors()}.
         *
         * @param poolSize The size of the thread pool to start with.
         * @return This builder.
         */
        public Builder poolSize(final int poolSize) {
            this.poolSize = checkNotNegative(poolSize, "poolSize");

            return this;
        }

        /**
         * The maximum number of threads to use with the client, defaults to
         * {@link Integer#MAX_VALUE}.
         *
         * @param maxPoolSize The maximum size to grow the thread pool to.
         * @return This builder.
         */
        public Builder maxPoolSize(final int maxPoolSize) {
            this.maxPoolSize = checkNotNegative(maxPoolSize, "maxPoolSize");

            return this;
        }

        /**
         * The Jackson JSON {@code ObjectMapper} to use when marshalling data to
         * and from the service, defaults to {@link io.orchestrate.client.JacksonMapper#builder()}.
         *
         * @param objectMapper A Jackson JSON {@code ObjectMapper}.
         * @return This builder.
         */
        public Builder mapper(final @NonNull ObjectMapper objectMapper) {
            return mapper(JacksonMapper.builder(objectMapper));
        }

        /**
         * A {@code Builder} used to build the {@code JacksonMapper} to use when
         * marshalling data to and from the service.
         *
         * @param mapperBuilder A {@code JacksonMapper.Builder}.
         * @return This builder.
         */
        public Builder mapper(final @NonNull JacksonMapper.Builder mapperBuilder) {
            return mapper(mapperBuilder.build());
        }

        /**
         * A {@code JacksonMapper} to use when marshalling data to and from the
         * service.
         *
         * @param mapper A {@code JacksonMapper}.
         * @return This builder.
         */
        public Builder mapper(final @NonNull JacksonMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        /**
         * Enable or disable SSL when connecting to the service, this value defaults
         * to {@code true}.
         *
         * @param useSSL If {@code true} enable SSL when connecting to the service.
         * @return This builder.
         */
        public Builder useSSL(final boolean useSSL) {
            this.useSSL = useSSL;
            return this;
        }

        /**
         * Set a custom value to be appended to the User-Agent header being sent
         * to Orchestrate.
         *
         * <p>This is useful for custom client implementations or libraries that
         * build on top of the Orchestrate client to help identify usage of those
         * implementations and libraries.
         *
         * @param userAgent The value to append as the "user-agent" request header.
         * @return This builder.
         */
        public Builder userAgent(final @NonNull String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public OrchestrateClient build() {
            return new OrchestrateClient(this);
        }

    }

    String uri(String... segments) {
        int length = 3 + segments.length;
        for(String segment:segments) {
            length += segment.length();
        }

        UEncoder encoder = ENCODER_HOLDER.get();

        StringBuilder buff = new StringBuilder(length).append("/v0");
        for(String segment:segments) {
            buff.append('/').append(encoder.encodeURL(segment));
        }
        return buff.toString();
    }

    String encode(String... segments) {
        UEncoder encoder = ENCODER_HOLDER.get();
        if(segments.length == 1){
            return encoder.encodeURL(segments[0]);
        }

        int length = segments.length - 1;
        for(String segment:segments) {
            length += segment.length();
        }

        StringBuilder buff = new StringBuilder(length);
        for(int i=0; i<segments.length; i++) {
            if(i != 0) {
                buff.append('/');
            }
            buff.append(encoder.encodeURL(segments[i]));
        }

        return buff.toString();
    }
}
