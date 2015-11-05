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

/**
 * The resource for the collection search features in the Orchestrate API.
 */
public class CollectionSearchResource extends BaseSearchResource {

    /** The name of the collection to search. */
    private final String collection;

    CollectionSearchResource(
            final OrchestrateClient client,
            final JacksonMapper mapper,
            final String collection) {
        super(client, mapper);
        assert (collection != null);
        assert (collection.length() > 0);

        this.collection = collection;
    }

    @Override
    protected String decorateUserQuery(String userQuery) {
        return userQuery;
    }

    @Override
    protected String makeTargetUri() {
        return client.uri(collection);
    }
}
