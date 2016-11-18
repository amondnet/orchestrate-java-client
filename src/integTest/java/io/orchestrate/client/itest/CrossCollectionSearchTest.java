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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.orchestrate.client.ItemKind;
import io.orchestrate.client.KvMetadata;
import io.orchestrate.client.KvObject;
import io.orchestrate.client.Relationship;
import io.orchestrate.client.RelationshipMetadata;
import io.orchestrate.client.Result;
import io.orchestrate.client.CrossCollectionSearchResource;
import io.orchestrate.client.SearchResults;

import java.util.Iterator;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * {@link io.orchestrate.client.OrchestrateClient#search()}.
 */
public final class CrossCollectionSearchTest extends BaseClientTest {

    @Test
    public void testSearchAcrossAllCollections() throws InterruptedException {

        String collectionA = collection("A");
        String collectionB = collection("B");

        // Insert items into two collections
        final KvMetadata aKvMetadata = insertItem(collectionA, "keyA", "{}");
        final KvMetadata bKvMetadata = insertItem(collectionB, "keyB", "{}");

        final String query = String.format(
            "@path.collection:`%s` OR @path.collection:`%s`", collectionA, collectionB
        );
        final SearchResults<String> results = search(query, "@path.key:asc", 2);

        final Iterator<Result<String>> iterator = results.iterator();

        Result<String> aResult = iterator.next();
        assertNotNull(aResult);
        final KvObject<String> aKvObject = aResult.getKvObject();
        assertNotNull(aKvObject);
        assertEquals(aKvMetadata.getCollection(), aKvObject.getCollection());
        assertEquals(aKvMetadata.getKey(), aKvObject.getKey());
        assertEquals(aKvMetadata.getRef(), aKvObject.getRef());
        assertEquals("{}", aKvObject.getValue());


        Result<String> bResult = iterator.next();
        assertNotNull(bResult);
        final KvObject<String> bKvObject = bResult.getKvObject();
        assertNotNull(bKvObject);
        assertEquals(bKvMetadata.getCollection(), bKvObject.getCollection());
        assertEquals(bKvMetadata.getKey(), bKvObject.getKey());
        assertEquals(bKvMetadata.getRef(), bKvObject.getRef());
        assertEquals("{}", bKvObject.getValue());
    }

    @Test
    public void searchRelationshipWithKindsMethod() throws InterruptedException {

        String collectionA = collection("A");
        String collectionB = collection("B");

        // Insert items into two collections
        final KvMetadata aKvMetadata = insertItem(collectionA, "keyA", "{}");
        final KvMetadata bKvMetadata = insertItem(collectionB, "keyB", "{}");

        // Insert a relationship into each collection
        final RelationshipMetadata abRelationshipMetadata = insertRelationship(aKvMetadata, bKvMetadata, "ab", null);
        final RelationshipMetadata baRelationshipMetadata = insertRelationship(aKvMetadata, bKvMetadata, "ba", null);

        // Search only for the relationship from A-to-B
        final SearchResults<Void> results = search()
                        .kinds(ItemKind.RELATIONSHIP)
                        .get("@path.relation:ab")
                        .get();

        assertEquals(1, results.getTotalCount());
        Result<Void> result = results.getResults().iterator().next();
        assertEquals(ItemKind.RELATIONSHIP, result.getKvObject().getItemKind());
        assertEquals(collectionA, ((Relationship) result.getKvObject()).getSourceCollection());
        assertEquals("keyA", ((Relationship) result.getKvObject()).getSourceKey());
        assertEquals("ab", ((Relationship) result.getKvObject()).getRelation());
        assertEquals(collectionB, ((Relationship) result.getKvObject()).getDestinationCollection());
        assertEquals("keyB", ((Relationship) result.getKvObject()).getDestinationKey());
    }

    private SearchResults<String> search(String query, String sort, int expectedCount) throws InterruptedException {
        SearchResults<String> results = search()
                .sort(sort)
                .get(String.class, query)
                .get();
        assertNotNull(results);
        assertEquals(expectedCount, results.getTotalCount());
        if (expectedCount > 0) {
            assertTrue(results.iterator().hasNext());
        }

        return results;
    }

    private CrossCollectionSearchResource search() throws InterruptedException {
        // give time for the write to hit the search index
        Thread.sleep(1000);
        return client.search();
    }

    protected String collection(String suffix) {
        String collection = name.getMethodName() + "-" + suffix;
        COLLECTIONS.add(collection);
        return collection;
    }

    protected KvObject<ObjectNode> readItem(String collection, String key) {
        return client.kv(collection, key)
                .get(ObjectNode.class)
                .get();
    }

    protected KvMetadata insertItem(String collection, String key, String json_ish, Object...args) {
        return client.kv(collection, key)
                .put(String.format(json_ish.replace('`', '"'), args))
                .get();
    }

    protected RelationshipMetadata insertRelationship(
        KvMetadata source, KvMetadata destination, String relation, String json_ish, Object...args
    ) {
        return client.relationship(source.getCollection(), source.getKey())
                .to(destination.getCollection(), destination.getKey())
                .put(relation, json_ish == null ? null : String.format(json_ish.replace('`', '"'), args))
                .get();
    }

}
