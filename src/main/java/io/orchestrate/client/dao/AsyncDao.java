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

import io.orchestrate.client.KvMetadata;
import io.orchestrate.client.KvObject;
import io.orchestrate.client.ResponseListener;

import javax.annotation.Nullable;

/**
 * Interface for generic Create, Read, Update, and Delete (CRUD) operations on
 * a collection for the specified type.
 *
 * @param <T> The type this object will control CRUD operations for.
 */
public interface AsyncDao<T> {

    // TODO add support for annotation-based store operations

    /**
     * Return the {@code KvObject} for the object stored with the specified
     * {@code key}.
     *
     * @param key The key used to find the object.
     * @return The KvObject with this key, may return {@code null}.
     */
    @Nullable
    KvObject<T> findOne(final String key);

    /**
     * Find the object stored with the specified {@code key} and fire the
     * {@code listeners} on the response.
     *
     * @param key The key used to find the object.
     * @param listeners The callbacks to fire on the operation's response.
     */
    void findOne(final String key, final Iterable<ResponseListener<KvObject<T>>> listeners);

    /**
     * Return the {@code KvObject} for the object stored with the specified
     * {@code key} and the version from the {@code metadata}.
     *
     * @param key The key used to find the object.
     * @param metadata The metadata about the version of the object to get.
     * @return The KvObject with this key and version (from the metadata), may
     *         return {@code null}.
     */
    @Nullable
    KvObject<T> findOne(final String key, final KvMetadata metadata);

    /**
     * Find the object stored with the specified {@code key} and the version
     * from the {@code metadata} and fire the {@code listeners} on the response.
     *
     * @param key The key used to find the object.
     * @param metadata The metadata about the version of the object to get.
     * @param listeners The callbacks to fire on the operation's response.
     */
    void findOne(final String key, final KvMetadata metadata, final Iterable<ResponseListener<KvObject<T>>> listeners);

    /**
     * Return the {@code KvObject} for the object stored with the specified
     * {@code key} and the version {@code ref}.
     *
     * @param key The key used to find the object.
     * @param ref The version of the object.
     * @return A KvObject with this key and version, may return {@code null}.
     */
    @Nullable
    KvObject<T> findOne(final String key, final String ref);

    /**
     * Find the object stored with the specified {@code key} and the version
     * {@code ref} and fire the {@code listeners} on the response.
     *
     * @param key The key used to find the object.
     * @param ref The version of the object.
     * @param listeners The callbacks to fire on the operation's response.
     */
    void findOne(final String key, final String ref, final Iterable<ResponseListener<KvObject<T>>> listeners);

    /**
     * Save the specified {@code value} to the {@code key}. This returns the
     * {@link KvMetadata} about the saved object.
     *
     * @param key The key to save the specified {@code value} to.
     * @param value The object to save.
     * @return The metadata about the saved object.
     */
    KvMetadata save(final String key, final T value);

    /**
     * Save the specified {@code value} to the {@code key} and fire the
     * {@code listeners} on the response.
     *
     * @param key The key to save the specified {@code value} to.
     * @param value The object to save.
     * @param listeners The callbacks to fire on the operation's response.
     */
    void save(final String key, final T value, final Iterable<ResponseListener<KvMetadata>> listeners);

    /**
     * Save the specified {@code value} to the {@code key} if and only if the
     * {@code key} does not already exist when {@code ifAbsent} is {@code true}.
     *
     * <p>If {@code ifAbsent} is {@code true} and the {@code key} already exists
     * the {@code KvMetadata} will be {@code null}.
     *
     * @param key The key to save the specified {@code value} to.
     * @param value The object to save.
     * @param ifAbsent When {@code true} the store operation will only succeed
     *                 if the specified {@code key} does not already exist.
     * @return The metadata about the saved object, may be {@code null}.
     */
    @Nullable
    KvMetadata save(final String key, final T value, final boolean ifAbsent);

    /**
     * Save the specified {@code value} to the {@code key} if and only if the
     * {@code key} does not already exist when {@code ifAbsent} is {@code true}.
     * Fire the {@code listeners} on the response.
     *
     * <p>If {@code ifAbsent} is {@code true} and the {@code key} already exists
     * the {@code KvMetadata} will be {@code null}.
     *
     * @param key The key to save the specified {@code value} to.
     * @param value The object to save.
     * @param ifAbsent When {@code true} the store operation will only succeed
     *                 if the specified {@code key} does not already exist.
     * @param listeners The callbacks to fire on the operation's response.
     */
    void save(final String key, final T value, final boolean ifAbsent, final Iterable<ResponseListener<KvMetadata>> listeners);

    /**
     * Save the specified {@code value} to the {@code key} if and only if the
     * ref from the {@code metadata} matches the current stored ref.
     *
     * <p>If the ref from the metadata does not match the current ref stored, the
     * resulting {@code KvMetadata} will be {@code null}.
     *
     * @param key The key to save the specified {@code value} to.
     * @param value The object to save.
     * @param metadata The metadata about the current object stored to this key.
     * @return The metadata about the saved object.
     */
    @Nullable
    KvMetadata save(final String key, final T value, final KvMetadata metadata);

    /**
     * Save the specified {@code value} to the {@code key} if and only if the
     * ref from the {@code metadata} matches the current stored ref. Fire the
     * {@code listeners} on the response.
     *
     * @param key The key to save the specified {@code value} to.
     * @param value The object to save.
     * @param metadata The metadata about the current object stored to this key.
     * @param listeners The callbacks to fire on the operation's response.
     */
    void save(final String key, final T value, final KvMetadata metadata, final Iterable<ResponseListener<KvMetadata>> listeners);

    /**
     * Save the specified {@code value} to the {@code key} if and only if the
     * {@code currentRef} matches the current stored ref.
     *
     * <p>If the {@code currentRef} does not match the current ref stored, the
     * resulting {@code KvMetadata} will be {@code null}.
     *
     * @param key The key to save the specified {@code value} to.
     * @param value The object to save.
     * @param currentRef The ref of the last known object stored to this key.
     * @return The metadata about the saved object.
     */
    @Nullable
    KvMetadata save(final String key, final T value, final String currentRef);

    /**
     * Save the specified {@code value} to the {@code key} if and only if the
     * {@code currentRef} matches the current stored ref. Fire the
     * {@code listeners} on the response.
     *
     * <p>If the {@code currentRef} does not match the current ref stored, the
     * resulting {@code KvMetadata} will be {@code null}.
     *
     * @param key The key to save the specified {@code value} to.
     * @param value The object to save.
     * @param currentRef The ref of the last known object stored to this key.
     * @param listeners The callbacks to fire on the operation's response.
     */
    void save(final String key, final T value, final String currentRef, final Iterable<ResponseListener<KvMetadata>> listeners);

    /**
     * Delete the object with the specified {@code key}.
     *
     * @param key The key of the object to delete.
     * @return The success of the delete.
     */
    Boolean delete(final String key);

    /**
     * Delete the object with the specified {@code key} and fire the
     * {@code listeners} on the response.
     *
     * @param key The key of the object to delete.
     * @param listeners The callbacks to fire on the operation's response.
     */
    void delete(final String key, final Iterable<ResponseListener<Boolean>> listeners);

    /**
     * Delete the object with the specified {@code key} if and only if the ref
     * from the {@code metadata} matches the current stored ref.
     *
     * <p>If the ref from the metadata does not match the current ref stored, the
     * result will be {@code false}.
     *
     * @param key The key of the object to delete.
     * @param metadata The metadata about the current object stored to this key.
     * @return The success of the delete.
     */
    Boolean delete(final String key, final KvMetadata metadata);

    /**
     * Delete the object with the specified {@code key} if and only if the ref
     * from the {@code metadata} matches the current stored ref.
     *
     * <p>If the ref from the metadata does not match the current ref stored, the
     * result will be {@code false}.
     *
     * @param key The key of the object to delete.
     * @param metadata The metadata about the current object stored to this key.
     * @param listeners The callbacks to fire on the operation's response.
     */
    void delete(final String key, final KvMetadata metadata, final Iterable<ResponseListener<Boolean>> listeners);

    /**
     * Delete the object with the specified {@code key} if and only if the
     * {@code currentRef} matches the current stored ref.
     *
     * <p>If the {@code currentRef} does not match the current ref stored, the
     * result will be {@code false}.
     *
     * @param key The key of the object to delete.
     * @param currentRef The ref of the last known object stored to this key.
     * @return The success of the delete.
     */
    Boolean delete(final String key, final String currentRef);

    /**
     * Delete the object with the specified {@code key} if and only if the
     * {@code currentRef} matches the current stored ref.
     *
     * <p>If the {@code currentRef} does not match the current ref stored, the
     * result will be {@code false}.
     *
     * @param key The key of the object to delete.
     * @param currentRef The ref of the last known object stored to this key.
     * @param listeners The callbacks to fire on the operation's response.
     */
    void delete(final String key, final String currentRef, final Iterable<ResponseListener<Boolean>> listeners);

    /**
     * Delete all objects in the collection.
     *
     * @return The success of the delete.
     */
    Boolean deleteCollection();

    /**
     * Delete all objects in the collection.
     *
     * @param listeners The callbacks to fire on the operation's response.
     */
    void deleteCollection(final Iterable<ResponseListener<Boolean>> listeners);

}
