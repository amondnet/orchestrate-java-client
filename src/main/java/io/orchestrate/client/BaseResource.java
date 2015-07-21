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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.utils.BufferInputStream;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * The base resource for features in the Orchestrate API.
 */
abstract class BaseResource {

    /** The Orchestrate client to make requests with. */
    protected final OrchestrateClient client;
    /** The object mapper used to deserialize JSON responses. */
    protected final ObjectMapper mapper;

    protected final JacksonMapper jacksonMapper;
    private static final Charset UTF8 = Charset.forName("UTF-8");

    BaseResource(final OrchestrateClient client, final JacksonMapper mapper) {
        assert (client != null);
        assert (mapper != null);

        this.client = client;
        this.jacksonMapper = mapper;
        this.mapper = mapper.getMapper();
    }

    protected byte[] toJsonBytes(Object value) {
        try {
            return (value instanceof String)
                    ? ((String) value).getBytes(UTF8)
                    : mapper.writeValueAsBytes(value);
        } catch (final Exception e) {
            throw new RuntimeException(e); // FIXME
        }
    }

    protected JsonNode toJsonNodeOrNull(HttpContent response) throws IOException {
        try {
            return mapper.readTree(new BufferInputStream(response.getContent()));
        } catch (JsonMappingException e) {
            // JsonMappingException is thrown whenever trying to parse an empty buffer
            return null;
        }
    }

    protected JsonNode toJsonNode(HttpContent response) throws IOException {
        return mapper.readTree(new BufferInputStream(response.getContent()));
    }

    protected <T> KvObject<T> toKvObject(JsonNode result, Class<T> clazz) throws IOException {
        return ResponseConverterUtil.wrapperJsonToKvObject(mapper, result, clazz);
    }

    protected <T> KvObject<T> toKvObject(HttpContent response, String collection, String key,
                                         Class<T> clazz) throws IOException {
        final String rawValue = response.getContent().toStringContent();
        final String ref = response.getHttpHeader().getHeader(Header.ETag)
                .replace("\"", "")
                .replaceFirst("-gzip$", "");

        return ResponseConverterUtil.jsonToKvObject(mapper, rawValue, clazz, collection, key, ref);
    }

}
