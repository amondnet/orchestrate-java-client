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

import org.glassfish.grizzly.http.HttpContent;

import java.io.IOException;

/**
 * A type converter that takes a HTTP response and converts it to type
 * {@code T}.
 *
 * @param <T> The type to deserialize the HTTP response to.
 */
interface ResponseConverter<T> {

    /**
     * Convert the HTTP response body to the type {@code T}.
     *
     * @param response The HTTP response.
     * @return The deserialized type.
     * @throws IOException If the HTTP response could not be deserialized.
     */
    T from(final HttpContent response) throws IOException;

}
