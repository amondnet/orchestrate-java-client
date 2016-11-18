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

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.attributes.AttributeHolder;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.impl.SafeFutureImpl;

/**
 * A grizzly adapter handler that writes the HTTP request to the connection.
 */
final class ConnectionCompletionHandler implements CompletionHandler<Connection> {

    /** The OrchestrateRequest object. */
    private final OrchestrateRequest orchestrateRequest;
    /** The HTTP request to write to the connection. */
    private final HttpContent rawRequest;

    public ConnectionCompletionHandler(
            final OrchestrateRequest orchestrateRequest,
            final HttpContent rawRequest) {
        assert (orchestrateRequest != null);
        assert (rawRequest != null);

        this.orchestrateRequest = orchestrateRequest;
        this.rawRequest = rawRequest;
    }

    /** {@inheritDoc} */
    @Override
    public void cancelled() {
        orchestrateRequest.cancel(false);
    }

    /** {@inheritDoc} */
    @Override
    public void failed(final Throwable t) {
        orchestrateRequest.failed(t);
    }

    /** {@inheritDoc} */
    @Override
    public void completed(final Connection conn) {
        final AttributeHolder attrs = conn.getAttributes();
        attrs.setAttribute(ClientFilter.OIO_RESPONSE_FUTURE_ATTR, orchestrateRequest.getRawResponseFuture());

        if(!orchestrateRequest.isCancelled()) {
            @SuppressWarnings("unchecked")
            final GrizzlyFuture write = conn.write(rawRequest);
            orchestrateRequest.setActiveRequest(write);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updated(final Connection connection) {
        // not used
    }

}
