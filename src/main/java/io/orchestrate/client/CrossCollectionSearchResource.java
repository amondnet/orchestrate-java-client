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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The resource for cross-collection search features in the Orchestrate API.
 */
public class CrossCollectionSearchResource extends BaseSearchResource {

    private Set<String> collections;

    CrossCollectionSearchResource(
            final OrchestrateClient client,
            final JacksonMapper mapper) {
        super(client, mapper);
        collections = new HashSet<String>();
    }

    @Override
    protected String makeTargetUri() {
        return client.uri();
    }

    @Override
    protected String decorateUserQuery(String userQuery) {
        if (collections.isEmpty()) {
            return userQuery;
        } else {
            // Make a list of verbatim collection name clauses, so that the final query looks like:
            //   @path.collection:(`A` `B` `C`) AND (foo bar baz)
            StringBuilder verbatimCollections =  new StringBuilder();
            for (String collection : collections) {
                if (verbatimCollections.length() > 0) {
                    verbatimCollections.append(' ');
                }
                verbatimCollections.append('`');
                verbatimCollections.append(collection);
                verbatimCollections.append('`');
            }
            return String.format("@path.collection:(%s) AND (%s)", verbatimCollections, userQuery);
        }
    }

    public CrossCollectionSearchResource collections(final String... collections) {
        this.collections.addAll(Arrays.asList(collections));
        return this;
    }
}
