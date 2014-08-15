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

import java.io.IOException;

/**
 * A client used to read and write data to the Orchestrate.io service.
 */
public interface Client {

    /**
     * Stops the thread pool and closes all connections in use by all the
     * operations.
     *
     * @throws IOException If resources couldn't be stopped.
     */
    public void close() throws IOException;

    /**
     * Delete all KV objects from a collection in the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * client.deleteCollection("someCollection").get();
     * }
     * </pre>
     *
     * @param collection The name of collection to delete.
     * @return The prepared request object.
     */
    public OrchestrateRequest<Boolean> deleteCollection(final String collection);

    /**
     * The resource for the event features in the Orchestrate API.
     *
     * @param collection The name of the collection.
     * @param key The name of the key with events.
     * @return The event resource.
     */
    public EventResource event(final String collection, final String key);

    /**
     * The resource for the KV features in the Orchestrate API.
     *
     * @param collection The name of the collection.
     * @param key The name of a key.
     * @return The KV resource.
     */
    public KvResource kv(final String collection, final String key);

    /**
     * The resource for the KV list features in the Orchestrate API.
     *
     * @param collection The name of the collection.
     * @return The KV list resource.
     */
    public KvListResource listCollection(final String collection);

    /**
     * Health check that sends a ping to the Orchestrate service.
     *
     * @throws IOException If the health check failed.
     */
    public void ping() throws IOException;

    /**
     * @see #ping()
     */
    @Deprecated
    public void ping(final String collection) throws IOException;

    /**
     * Store an object by value in the collection specified to the Orchestrate service
     * which will auto-generate a key for it.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * DomainObject obj = new DomainObject();
     * KvMetadata kvMetadata =
     *         client.postValue("someCollection", obj).get();
     * }
     * </pre>
     *
     * @param collection The name of the collection.
     * @param value The object to store.
     * @return The prepared put request.
     * @throws IOException If the request failed.
     */
    public OrchestrateRequest<KvMetadata> postValue(final String collection, final Object value) throws IOException;

    /**
     * The resource for the relation features in the Orchestrate API.
     *
     * @param collection The name of the collection.
     * @param key The name of the key.
     * @return The relation resource.
     */
    public RelationResource relation(final String collection, final String key);

    /**
     * The resource for the collection search features in the Orchestrate API.
     *
     * @param collection The name of the collection to search.
     * @return The collection search resource.
     */
    public CollectionSearchResource searchCollection(final String collection);

}
