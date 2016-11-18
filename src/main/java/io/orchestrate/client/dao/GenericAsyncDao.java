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
package io.orchestrate.client.dao;

import io.orchestrate.client.*;

import static io.orchestrate.client.Preconditions.checkNotNull;
import static io.orchestrate.client.Preconditions.checkNotNullOrEmpty;

/**
 * A generic object for CRUD data access operations.
 *
 * @param <T> The type this object will control CRUD operations for.
 *
 * @see <a href="http://java.orchestrate.io/examples/#data-access-object">http://java.orchestrate.io/examples/#data-access-object</a>
 */
public abstract class GenericAsyncDao<T> implements AsyncDao<T> {

    /** The client to query the Orchestrate.io service. */
    private final Client client;
    /** The name of the collection to store objects to. */
    private final String collection;
    /** Type information for marshalling objects at runtime. */
    private final Class<T> clazz;

    /**
     * Create a data access object to store objects of type {@code T} to the
     * specified {@code collection} using the {@code client}.
     *
     * @param client The client used to query the Orchestrate.io service.
     * @param collection The name of the collection to store objects to.
     * @param clazz Type information for deserializing to type {@code T} at
     *              runtime.
     */
    public GenericAsyncDao(final Client client, final String collection, final Class<T> clazz) {
        this.collection = checkNotNullOrEmpty(collection, "collection");
        this.client = checkNotNull(client, "client");
        this.clazz = checkNotNull(clazz, "clazz");
    }

    /** {@inheritDoc} */
    @Override
    public KvObject<T> findOne(final String key) {
        return client.kv(collection, key).get(clazz).get();
    }

    /** {@inheritDoc} */
    @Override
    public void findOne(final String key, final Iterable<ResponseListener<KvObject<T>>> listeners) {
        client.kv(collection, key).get(clazz).on(listeners);
    }

    /** {@inheritDoc} */
    @Override
    public KvObject<T> findOne(final String key, final KvMetadata metadata) {
        checkNotNull(metadata, "metadata");
        return findOne(key, metadata.getRef());
    }

    /** {@inheritDoc} */
    @Override
    public void findOne(
            final String key,
            final KvMetadata metadata,
            final Iterable<ResponseListener<KvObject<T>>> listeners) {
        checkNotNull(metadata, "metadata");
        findOne(key, metadata.getRef(), listeners);
    }

    /** {@inheritDoc} */
    @Override
    public KvObject<T> findOne(final String key, final String ref) {
        return client.kv(collection, key).get(clazz, ref).get();
    }

    /** {@inheritDoc} */
    @Override
    public void findOne(
            final String key,
            final String ref,
            final Iterable<ResponseListener<KvObject<T>>> listeners) {
        client.kv(collection, key).get(clazz, ref).on(listeners);
    }

    /** {@inheritDoc} */
    @Override
    public KvMetadata save(final String key, final T value) {
        return client.kv(collection, key).put(value).get();
    }

    /** {@inheritDoc} */
    @Override
    public void save(
            final String key,
            final T value,
            final Iterable<ResponseListener<KvMetadata>> listeners) {
        client.kv(collection, key).put(value).on(listeners);
    }

    /** {@inheritDoc} */
    @Override
    public KvMetadata save(
            final String key,
            final T value,
            final boolean ifAbsent) {
        return client.kv(collection, key).ifAbsent(ifAbsent).put(value).get();
    }

    /** {@inheritDoc} */
    @Override
    public void save(
            final String key,
            final T value,
            final boolean ifAbsent,
            final Iterable<ResponseListener<KvMetadata>> listeners) {
        client.kv(collection, key).ifAbsent(ifAbsent).put(value).on(listeners);
    }

    /** {@inheritDoc} */
    @Override
    public KvMetadata save(
            final String key,
            final T value,
            final KvMetadata metadata) {
        checkNotNull(metadata, "metadata");
        return save(key, value, metadata.getRef());
    }

    /** {@inheritDoc} */
    @Override
    public void save(
            final String key,
            final T value,
            final KvMetadata metadata,
            final Iterable<ResponseListener<KvMetadata>> listeners) {
        checkNotNull(metadata, "metadata");
        save(key, value, metadata.getRef(), listeners);
    }

    /** {@inheritDoc} */
    @Override
    public KvMetadata save(
            final String key,
            final T value,
            final String currentRef) {
        return client.kv(collection, key).ifMatch(currentRef).put(value).get();
    }

    /** {@inheritDoc} */
    @Override
    public void save(
            final String key,
            final T value,
            final String currentRef,
            final Iterable<ResponseListener<KvMetadata>> listeners) {
        client.kv(collection, key).ifMatch(currentRef).put(value).on(listeners);
    }

    /** {@inheritDoc} */
    @Override
    public Boolean delete(final String key) {
        return client.kv(collection, key).delete().get();
    }

    /** {@inheritDoc} */
    @Override
    public void delete(
            final String key,
            final Iterable<ResponseListener<Boolean>> listeners) {
        client.kv(collection, key).delete().on(listeners);
    }

    /** {@inheritDoc} */
    @Override
    public Boolean delete(final String key, final KvMetadata metadata) {
        checkNotNull(metadata, "metadata");
        return delete(key, metadata.getRef());
    }

    /** {@inheritDoc} */
    @Override
    public void delete(
            final String key,
            final KvMetadata metadata,
            final Iterable<ResponseListener<Boolean>> listeners) {
        checkNotNull(metadata, "metadata");
        delete(key, metadata.getRef(), listeners);
    }

    /** {@inheritDoc} */
    @Override
    public Boolean delete(final String key, final String currentRef) {
        return client.kv(collection, key).ifMatch(currentRef).delete().get();
    }

    /** {@inheritDoc} */
    @Override
    public void delete(
            final String key,
            final String currentRef,
            final Iterable<ResponseListener<Boolean>> listeners) {
        client.kv(collection, key).ifMatch(currentRef).delete().on(listeners);
    }

    /** {@inheritDoc} */
    @Override
    public Boolean deleteCollection() {
        return client.deleteCollection(collection).get();
    }

    /** {@inheritDoc} */
    @Override
    public void deleteCollection(final Iterable<ResponseListener<Boolean>> listeners) {
        client.deleteCollection(collection).on(listeners);
    }

}
