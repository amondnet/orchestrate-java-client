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
package io.orchestrate.client.itest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.orchestrate.client.Client;
import io.orchestrate.client.KvMetadata;
import io.orchestrate.client.KvObject;
import io.orchestrate.client.OrchestrateClient;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * A base test class for testing the {@code OrchestrateClient}.
 */
public abstract class BaseClientTest {
    @Rule
    public TestName name = new TestName();

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected static final Set<String> COLLECTIONS = new HashSet<String>();
    protected static final Random RAND = new Random();

    /** The client instance to use with requests to the Orchestrate service. */
    protected static Client client;

    @BeforeClass
    public static void setUpClass() {
        final String apiKey = System.getenv("ORCHESTRATE_API_KEY");
        if (apiKey == null || apiKey.length() < 1) {
            throw new IllegalStateException(
                    "Cannot run integration tests, the environment variable 'ORCHESTRATE_API_KEY' is blank.");
        }

        String orchestrate_api_endpoint = System.getenv("ORCHESTRATE_API_ENDPOINT");
        if (orchestrate_api_endpoint == null || orchestrate_api_endpoint.length() < 1)
            orchestrate_api_endpoint = OrchestrateClient.Builder.DEFAULT_HOST;

        URI uri = URI.create(orchestrate_api_endpoint);

        String host = uri.getScheme()+"://" + uri.getHost();
        int port = uri.getPort();
        if(port == -1) {
            if( uri.getScheme().equals("https")) {
                port = 443;
            } else {
                port = 80;
            }
        }
        boolean ssl = uri.getScheme().equals("https");

        client = OrchestrateClient.builder(apiKey)
                .host(host)
                .port(port)
                .useSSL(ssl)
                .build();
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        for (final String collection : COLLECTIONS) {
            client.deleteCollection(collection).get();
        }
        client.close();
    }

    protected String collection() {
        COLLECTIONS.add(name.getMethodName());
        return name.getMethodName();
    }


    protected KvObject<ObjectNode> readItem(String key) {
        return client.kv(collection(), key)
                .get(ObjectNode.class)
                .get();
    }

    protected KvMetadata insertItem(String key, String json_ish, Object...args) {
        return client.kv(collection(), key)
                .put(String.format(json_ish.replace('`', '"'), args))
                .get();
    }
}
