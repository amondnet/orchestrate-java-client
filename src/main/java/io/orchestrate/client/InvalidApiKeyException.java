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

/**
 * The InvalidApiKeyException is thrown on any client request when the
 * Orchestrate Api Key is invalid. For example, calling the (blocking)
 * ping() method on client:
 *
 * <pre>
 * {@code
 *   try {
 *     client.ping();
 *   } catch (InvalidApiKeyException ex) {
 *     //do something with invalid Api Key failures
 *   }
 * }
 * </pre>
 *
 * Or, via a blocking get()
 *
 * <pre>
 * {@code
 *   try {
 *     client.kv("collection", "key")
 *     .get(String.class)
 *     .get();
 *   } catch (InvalidApiKeyException ex) {
 *     //do something with invalid Api Key failures
 *   }
 * }
 * </pre>
 *
 * Any Listeners attached for asynchronous handling will be failed and
 * the 'onFailure' method will be called with the InvalidApiKeyException:
 *
 * <pre>
 * {@code
 *   client.kv("collection", "key")
 *   .get(String.class)
 *   .on(new ResponseListener<KvObject<String>>() {
 *     public void onFailure(Throwable failure) {
 *       if (failure instanceof InvalidApiKeyException) {
 *          //do something with invalid Api Key failures
 *       }
 *     }
 *     public void onSuccess(KvObject<String> object) {
 *     }
 *   });
 * }
 * </pre>
 */
@SuppressWarnings("serial")
public class InvalidApiKeyException extends RequestException {
    InvalidApiKeyException(int statusCode, JsonNode json, String rawResponse, String requestId) {
        super(statusCode, json, rawResponse, requestId);
    }
}
