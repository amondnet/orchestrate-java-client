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

import com.fasterxml.jackson.databind.JsonNode;
import com.pholser.junit.quickcheck.ForAll;
import io.orchestrate.client.*;
import org.glassfish.grizzly.utils.DataStructures;
import org.junit.Test;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeThat;

/**
 * {@link io.orchestrate.client.OrchestrateClient#listCollection(String)}.
 */
@RunWith(Theories.class)
public final class KvListTest extends BaseClientTest {

    @Theory
    public void getList(@ForAll(sampleSize=10) final String key) {
        assumeThat(key, not(isEmptyString()));

        final KvMetadata kvMetadata = insertItem("key", "{}");

        final KvList<String> kvList =
                client.listCollection(kvMetadata.getCollection())
                      .limit(1)
                      .get(String.class)
                      .get();

        assertNotNull(kvMetadata);
        assertNotNull(kvList);
        assertTrue(kvList.iterator().hasNext());

        final KvObject<String> kvObject = kvList.iterator().next();
        assertNotNull(kvObject);
        assertEquals(kvMetadata.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata.getKey(), kvObject.getKey());
        assertEquals(kvMetadata.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());
    }

    @Test
    public void listItemsAndApplyWhitelistFieldFiltering() throws InterruptedException, IOException {
        String key = Long.toHexString(RAND.nextLong());
        KvMetadata kvMetadata = insertItem(key, "{`foo`:`bar`,`bing`:`bong`,`zip`:`zap`}");

        KvList<JsonNode> kvList = client.listCollection(kvMetadata.getCollection())
            .limit(1)
            .withFields("value.foo")
            .get(JsonNode.class)
            .get();

        assertNotNull(kvMetadata);
        assertNotNull(kvList);
        assertTrue(kvList.iterator().hasNext());

        KvObject<JsonNode> kvObject = kvList.iterator().next();

        JsonNode resultJson = kvObject.getValue();
        assertEquals("bar", resultJson.get("foo").asText());
        assertFalse(resultJson.has("bing"));
        assertFalse(resultJson.has("zip"));
    }

    @Test
    public void listItemsAndApplyBlacklistFieldFiltering() throws InterruptedException, IOException {
        String key = Long.toHexString(RAND.nextLong());
        KvMetadata kvMetadata = insertItem(key, "{`foo`:`bar`,`bing`:`bong`,`zip`:`zap`}");

        KvList<JsonNode> kvList = client.listCollection(kvMetadata.getCollection())
            .limit(1)
            .withoutFields("value.foo")
            .get(JsonNode.class)
            .get();

        assertNotNull(kvMetadata);
        assertNotNull(kvList);
        assertTrue(kvList.iterator().hasNext());

        KvObject<JsonNode> kvObject = kvList.iterator().next();

        JsonNode resultJson = kvObject.getValue();
        assertEquals("bong", resultJson.get("bing").asText());
        assertEquals("zap", resultJson.get("zip").asText());
        assertFalse(resultJson.has("foo"));
    }

    @Theory
    public void getListAsync(@ForAll(sampleSize=10) final String key)
            throws InterruptedException {
        assumeThat(key, not(isEmptyString()));

        final KvMetadata kvMetadata = insertItem("key", "{}");

        final BlockingQueue<KvList> queue = DataStructures.getLTQInstance(KvList.class);
        client.listCollection(kvMetadata.getCollection())
              .limit(1)
              .get(String.class)
              .on(new ResponseAdapter<KvList<String>>() {
                  @Override
                  public void onSuccess(final KvList<String> object) {
                      queue.add(object);
                  }

                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }
              });

        @SuppressWarnings("unchecked")
        final KvList<String> kvList = queue.poll(5000, TimeUnit.MILLISECONDS);

        assertNotNull(kvMetadata);
        assertNotNull(kvList);
        assertTrue(kvList.iterator().hasNext());

        final KvObject<String> kvObject = kvList.iterator().next();
        assertNotNull(kvObject);
        assertEquals(kvMetadata.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata.getKey(), kvObject.getKey());
        assertEquals(kvMetadata.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());
    }

    @Test
    public void getListAndPaginate() {
        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");

        final KvList<String> kvList1 =
                client.listCollection(collection())
                      .limit(1)
                      .get(String.class)
                      .get();

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertNotNull(kvList1);
        assertTrue(kvList1.iterator().hasNext());

        final KvObject<String> kvObject1 = kvList1.iterator().next();
        assertNotNull(kvObject1);
        assertTrue(kvList1.hasNext());

        assertFalse(kvList1.getNext().hasSent());
        final KvList<String> kvList2 = kvList1.getNext().get();
        assertNotNull(kvList2);
        assertTrue(kvList2.iterator().hasNext());

        final KvObject<String> kvObject2 = kvList2.iterator().next();
        assertNotNull(kvObject2);
    }

    @Test
    public void getListWithoutValues() {
        final String collection = collection();
        final KvMetadata kvMetadata = insertItem("key1", "{}");

        final KvList<String> kvList =
                client.listCollection(collection)
                      .withValues(false)
                      .get(String.class)
                      .get();

        assertNotNull(kvMetadata);
        assertNotNull(kvList);
        assertTrue(kvList.iterator().hasNext());

        final KvObject<String> kvObject = kvList.iterator().next();
        assertEquals(collection, kvObject.getCollection());
        assertEquals("key1", kvObject.getKey());
        assertEquals(kvMetadata.getRef(), kvObject.getRef());
        assertNull(kvObject.getValue());
    }

    @Test
    public void getListWithStartAndEndNonInclusive() {
        final String collection = collection();
        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");
        final KvMetadata kvMetadata3 = insertItem("key3", "{}");

        final KvList<String> kvList =
                client.listCollection(collection)
                        .withValues(false)
                        .startKey("key1")
                        .stopKey("key3")
                        .get(String.class)
                        .get();

        assertNotNull(kvMetadata2);
        assertNotNull(kvList);
        assertTrue(kvList.iterator().hasNext());

        final KvObject<String> kvObject = kvList.iterator().next();
        assertEquals(collection, kvObject.getCollection());
        assertEquals("key2", kvObject.getKey());
        assertEquals(kvMetadata2.getRef(), kvObject.getRef());
        assertNull(kvObject.getValue());
    }

    @Test
    public void getListWithStartAndEndInclusive() {
        final String collection = collection();
        List<KvMetadata> allMeta = new ArrayList<KvMetadata>();
        allMeta.add(insertItem("key0", "{}"));
        allMeta.add(insertItem("key1", "{}"));
        allMeta.add(insertItem("key2", "{}"));
        allMeta.add(insertItem("key3", "{}"));
        allMeta.add(insertItem("key4", "{}"));

        final KvList<String> kvList =
                client.listCollection(collection)
                        .withValues(false)
                        .startKey("key1", true)
                        .stopKey("key3", true)
                        .get(String.class)
                        .get();

        assertNotNull(kvList);

        Iterator<KvObject<String>> kvResultIter = kvList.iterator();
        List<String> keys = new ArrayList<String>();

        assertTrue(kvResultIter.hasNext());
        for(int i=1; i<4; i++) {
            final KvObject<String> result = kvResultIter.next();
            final KvMetadata expected = allMeta.get(i);

            keys.add(result.getKey());
            assertEquals(expected.getCollection(), result.getCollection());
            assertEquals(expected.getKey(), result.getKey());
            assertEquals(expected.getRef(), result.getRef());
            assertNull(result.getValue());
        }

        assertFalse(kvResultIter.hasNext());
        assertEquals(Arrays.asList("key1", "key2", "key3"), keys);
    }
}
