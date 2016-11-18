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

import lombok.EqualsAndHashCode;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.http.HttpContent;

import java.io.IOException;

/**
 * A listener that proxies the Grizzly NIO completion event to a
 * {@code ResponseListener}.
 *
 * @param <T> The type to deserialize the HTTP response into.
 */
@EqualsAndHashCode
final class ResponseCompletionHandler<T> implements CompletionHandler<T> {

    /** The listener to proxy the HTTP response to. */
    private final ResponseListener<T> listener;

    public ResponseCompletionHandler(final ResponseListener<T> listener) {
        assert (listener != null);

        this.listener = listener;
    }

    /** {@inheritDoc} */
    @Override
    public void cancelled() {
        // not used
    }

    /** {@inheritDoc} */
    @Override
    public void failed(final Throwable throwable) {
        listener.onFailure(throwable);
    }

    /** {@inheritDoc} */
    @Override
    public void completed(final T result) {
        listener.onSuccess(result);
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final T result) {
        // not used
    }

}
