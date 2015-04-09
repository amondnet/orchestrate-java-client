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

import io.orchestrate.client.*;
import org.glassfish.grizzly.utils.DataStructures;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * {@link io.orchestrate.client.OrchestrateClient#searchCollection(String)}.
 */
public final class SearchTest extends BaseClientTest {
    @Test
    public void getSearchCollectionEmpty() throws InterruptedException {
        final SearchResults<String> results = search("*", 0);

        assertNotNull(results);
        assertFalse(results.iterator().hasNext());
    }

    @Test
    public void getSearchCollection() throws InterruptedException {
        final KvMetadata kvMetadata = insertItem("key", "{}");

        final SearchResults<String> results = search("*", 1);

        final Result<String> result = results.iterator().next();
        assertNotNull(result);

        final KvObject<String> kvObject = result.getKvObject();
        assertNotNull(kvObject);
        assertEquals(kvMetadata.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata.getKey(), kvObject.getKey());
        assertEquals(kvMetadata.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());
    }

    @Test
    public void getSearchCollectionAsync() throws InterruptedException {
        final KvMetadata kvMetadata = insertItem("key", "{}");

        final BlockingQueue<SearchResults> queue = DataStructures.getLTQInstance(SearchResults.class);
        search()
              .get(String.class, "*")
              .on(new ResponseAdapter<SearchResults<String>>() {
                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }

                  @Override
                  public void onSuccess(final SearchResults<String> object) {
                      queue.add(object);
                  }
              });

        @SuppressWarnings("unchecked")
        final SearchResults<String> results = queue.poll(5000, TimeUnit.MILLISECONDS);

        assertTrue(results.iterator().hasNext());

        final Result<String> result = results.iterator().next();
        assertNotNull(result);

        final KvObject<String> kvObject = result.getKvObject();
        assertNotNull(kvObject);
        assertEquals(kvMetadata.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata.getKey(), kvObject.getKey());
        assertEquals(kvMetadata.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());
    }

    @Test
    public void getSearchCollectionAndPaginate() throws InterruptedException {
        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");

        final SearchResults<String> results1 = search()
                      .limit(10)
                      .offset(0)
                      .sort("key")
                      .get(String.class, "*")
                      .get();

        final SearchResults<String> results2 = search()
                      .offset(1)
                      .sort("key")
                      .get(String.class, "*")
                      .get();

        assertNotNull(results1);
        assertNotNull(results2);
        assertTrue(results1.iterator().hasNext());
        assertTrue(results2.iterator().hasNext());

        final Result<String> result1 = results1.iterator().next();
        assertNotNull(result1);

        final KvObject<String> kvObject1 = result1.getKvObject();
        assertNotNull(kvObject1);
        assertEquals(kvMetadata1.getCollection(), kvObject1.getCollection());
        assertEquals(kvMetadata1.getKey(), kvObject1.getKey());
        assertEquals(kvMetadata1.getRef(), kvObject1.getRef());
        assertEquals("{}", kvObject1.getValue());

        final Result<String> result2 = results2.iterator().next();
        assertNotNull(result2);

        final KvObject<String> kvObject2 = result2.getKvObject();
        assertNotNull(kvObject2);
        assertEquals(kvMetadata2.getCollection(), kvObject2.getCollection());
        assertEquals(kvMetadata2.getKey(), kvObject2.getKey());
        assertEquals(kvMetadata2.getRef(), kvObject2.getRef());
        assertEquals("{}", kvObject2.getValue());
    }

    @Test
    public void getSearchResultsWithoutValues() throws InterruptedException {
        final KvMetadata kvMetadata = insertItem("key1", "{}");

        final SearchResults<String> results = search()
                        .withValues(false)
                        .get(String.class, "*")
                        .get();

        assertNotNull(results);
        assertTrue(results.iterator().hasNext());

        final Result<String> result = results.iterator().next();
        assertNotNull(result);

        final KvObject<String> kvObject = result.getKvObject();
        assertNotNull(kvObject);
        assertEquals(kvMetadata.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata.getKey(), kvObject.getKey());
        assertEquals(kvMetadata.getRef(), kvObject.getRef());
        assertNull(kvObject.getValue());
    }

    @Test
    public void getSearchResultsSorted() throws InterruptedException {
        insertItem("key1", "{`name`: {`first`: `Jacob`, `last`: `Grimm`}}");
        insertItem("key2", "{`name`: {`first`: `Wilhelm`, `last`: `Grimm`}}");

        final SearchResults<String> results = search()
                        .sort("value.name.last:asc,value.name.first:asc")
                        .get(String.class, "value.name.last: Grimm")
                        .get();

        assertNotNull(results);
        assertTrue(results.iterator().hasNext());

        final Result<String> result = results.iterator().next();
        assertNotNull(result);

        final KvObject<String> kvObject = result.getKvObject();
        assertNotNull(kvObject);
        assertTrue(kvObject.getValue().contains("Jacob"));
    }

    @Test
    public void getSearchResultsFromGeoQuery() throws InterruptedException {
        final KvMetadata kvMetadata = insertItem("key1", "{`location`: {`lat`: 1, `lon`: 1}}");
        final SearchResults<String> results = search()
                        .sort("value.location:distance:asc")
                        .get(String.class, "value.location:NEAR:{lat:1 lon:1 dist:1km}")
                        .get();

        assertNotNull(kvMetadata);
        assertNotNull(results);
        assertTrue(results.iterator().hasNext());

        final Result<String> result = results.iterator().next();
        assertNotNull(result);
        final KvObject<String> kvObject = result.getKvObject();
        assertNotNull(kvObject);
        System.out.println(result);
        assertEquals(new Double(0), result.getDistance());
    }

    @Test
    public void searchEvents() throws InterruptedException {
        String collection = collection();
        int numEvents = 5;
        for(int i=0;i<numEvents;i++) {
                client.event(collection, "key")
                        .type("type_1")
                        .create("{}")
                        .get();
        }
        final SearchResults<Void> results = search()
                        .get("@path.kind:event")
                        .get();

        assertEquals(numEvents, results.getTotalCount());
        for(Result<Void> result : results.getResults()) {
            assertTrue(result.getKvObject() instanceof Event);
            assertEquals("type_1", ((Event)result.getKvObject()).getType());
        }
    }

    @Test
    public void searchEventsWithKindsMethod() throws InterruptedException {
        String collection = collection();
        int numEvents = 5;
        for(int i=0;i<numEvents;i++) {
            client.event(collection, "key")
                    .type("type_1")
                    .create("{}")
                    .get();
        }

        final SearchResults<Void> results = search()
                        .kinds("event")
                        .get("*")
                        .get();

        assertEquals(numEvents, results.getTotalCount());
        for(Result<Void> result : results.getResults()) {
            assertTrue(result.getKvObject() instanceof Event);
            assertEquals("type_1", ((Event)result.getKvObject()).getType());
        }
    }

    @Test
    public void searchEventsByType() throws InterruptedException {
        String collection = collection();
        int numEvents = 5;
        for(int i=0;i<numEvents;i++) {
            client.event(collection, "key")
                    .type("type_"+i)
                    .create("{}")
                    .get();
        }

        final SearchResults<Void> results = search()
                        .get("@path.kind:event AND @path.type:type_1")
                        .get();

        assertEquals(1, results.getTotalCount());
        Result<Void> result = firstResult(results);
        assertEquals("type_1", ((Event)result.getKvObject()).getType());
    }

    @Test
    public void searchEventsAndItems() throws InterruptedException {
        String collection = collection();
        int numValues = 5;
        for(int i=0;i<numValues;i++) {
            String key = Long.toHexString(RAND.nextLong());
            String value = "{\"val\":\"val_"+i+"\"}";

            // write item with the test value.
            insertItem(key, value);

            // write events to another key, with the same value.
            key = Long.toHexString(RAND.nextLong());
            client.event(collection, key)
                    .type("type_1")
                    .create(value)
                    .get();
        }

        final SearchResults<Void> results = search()
                        .sort("@path.kind")  // this is so the event will come first.
                        .get("@path.kind:(item event) AND val:`val_1`")
                        .get();

        assertEquals(2, results.getTotalCount());
        List<Result<Void>> resultList = toList(results);
        assertTrue(resultList.get(0).getKvObject() instanceof Event);
        assertFalse(resultList.get(1).getKvObject() instanceof Event);
    }

    @Test
    public void searchEventsAndItemsViaKindsMethod() throws InterruptedException {
        String collection = collection();
        int numValues = 5;
        for(int i=0;i<numValues;i++) {
            String key = Long.toHexString(RAND.nextLong());
            String value = "{\"val\":\"val_"+i+"\"}";

            // write item with the test value.
            insertItem(key, value);

            // write events to another key, with the same value.
            key = Long.toHexString(RAND.nextLong());
            client.event(collection, key)
                    .type("type_1")
                    .create(value)
                    .get();
        }
        final SearchResults<Void> results = search()
                        .sort("@path.kind")  // this is so the event will come first.
                        .kinds("item", "event")
                        .get("val:`val_1`")
                        .get();

        assertEquals(2, results.getTotalCount());
        List<Result<Void>> resultList = toList(results);
        assertTrue(resultList.get(0).getKvObject() instanceof Event);
        assertFalse(resultList.get(1).getKvObject() instanceof Event);
    }

    @Test
    public void searchEventsAndItemsPojo() throws InterruptedException, IOException {
        String collection = collection();
        int numValues = 5;
        for(int i=0;i<numValues;i++) {
            String key = Long.toHexString(RAND.nextLong());
            String desc = "This is description test_"+i;

            // write item with the test desc.
            client.kv(collection, key).put(new User(key, desc)).get();

            // write events to another key, with the same desc.
            key = Long.toHexString(RAND.nextLong());
            client.event(collection, key)
                    .type("type_1")
                    .create(new LogItem(key, desc))
                    .get();
        }

        final SearchResults<Void> results = search()
                        .sort("@path.kind")  // this is so the event will come first.
                        .get("@path.kind:(item event) AND description:test_1")
                        .get();

        assertEquals(2, results.getTotalCount());
        List<Result<Void>> resultList = toList(results);

        assertTrue(resultList.get(0).getKvObject() instanceof Event);
        LogItem log = resultList.get(0).getKvObject().getValue(LogItem.class);
        assertTrue(log.getDescription().contains("test_1"));

        assertFalse(resultList.get(1).getKvObject() instanceof Event);
        User user = resultList.get(1).getKvObject().getValue(User.class);
        assertTrue(user.getDescription().contains("test_1"));
    }

    private <T> Result<T> firstResult(SearchResults<T> results) {
        Iterator<Result<T>> iter = results.getResults().iterator();
        if (iter.hasNext()) {
            return iter.next();
        }
        return null;
    }

    private <T> List<Result<T>> toList(SearchResults<T> results) {
        List<Result<T>> l = new ArrayList<Result<T>>(results.getCount());
        for (Result<T> r : results.getResults()) {
            l.add(r);
        }
        return l;
    }

    private SearchResults<String> search(String query, int expectedCount) throws InterruptedException {
        SearchResults<String> results = search()
                .get(String.class, query)
                .get();
        assertNotNull(results);
        assertEquals(expectedCount, results.getTotalCount());
        if (expectedCount > 0) {
            assertTrue(results.iterator().hasNext());
        }

        return results;
    }

    private CollectionSearchResource search() throws InterruptedException {
        // give time for the write to hit the search index
        Thread.sleep(1000);
        return client.searchCollection(collection());
    }

}
