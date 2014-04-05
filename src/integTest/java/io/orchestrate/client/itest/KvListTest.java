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

import com.google.common.io.BaseEncoding;
import com.pholser.junit.quickcheck.ForAll;
import io.orchestrate.client.*;
import org.glassfish.grizzly.utils.DataStructures;
import org.junit.BeforeClass;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.runner.RunWith;

import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;

/**
 * {@link io.orchestrate.client.OrchestrateClient#list(String)}.
 */
@RunWith(Theories.class)
public final class KvListTest {
    private static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();

    /** The client to run tests on. */
    private static Client client;

    @BeforeClass
    public static void setUpClass() {
        client = KvTest.createClient();
    }

    @Theory
    public void getList(@ForAll(sampleSize=10) final String collection) {
        assumeThat(collection, not(isEmptyString()));

        final KvMetadata kvMetadata =
                client.kv(collection, "key")
                      .put("{}")
                      .execute();

        final KvList<String> kvList =
                client.list(kvMetadata.getCollection())
                      .limit(1)
                      .get(String.class)
                      .execute();

        assertNotNull(kvMetadata);
        assertNotNull(kvList);
        assertTrue(kvList.iterator().hasNext());

        final KvObject<String> kvObject = kvList.iterator().next();
        assertNotNull(kvObject);
        assertEquals("KvMetadata collection should match", collection, kvMetadata.getCollection());

        StringBuilder buff = new StringBuilder();
        String collectionHex = HEX.encode(collection.getBytes(Charset.forName("UTF-8")));
        String kvCollectionHex = HEX.encode(kvObject.getCollection().getBytes(Charset.forName("UTF-8")));
        buff.append("UTF-8 hex ["+collectionHex+","+kvCollectionHex+"] ("+(collectionHex.equals(kvCollectionHex)?"equals":"NOT equal")+") ");

        String collectionHex2 = HEX.encode(collection.getBytes(Charset.forName("UTF-16")));
        String kvCollectionHex2 = HEX.encode(kvObject.getCollection().getBytes(Charset.forName("UTF-16")));
        buff.append("UTF-16 hex ["+collectionHex2+","+kvCollectionHex2+"] ("+(collectionHex2.equals(kvCollectionHex2)?"equals":"NOT equal")+") ");

        assertEquals("Collection Names not equal. "+buff.toString(), kvMetadata.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata.getKey(), kvObject.getKey());
        assertEquals(kvMetadata.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());
    }

    @Theory
    public void getListAsync(@ForAll(sampleSize=10) final String collection)
            throws InterruptedException {
        assumeThat(collection, not(isEmptyString()));

        final KvMetadata kvMetadata =
                client.kv(collection, "key")
                      .put("{}")
                      .execute();

        final BlockingQueue<KvList> queue = DataStructures.getLTQInstance(KvList.class);
        client.list(kvMetadata.getCollection())
              .limit(1)
              .get(String.class)
              .executeAsync(new ResponseAdapter<KvList<String>>() {
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

    @Theory
    public void getListAndPaginate(@ForAll(sampleSize=10) final String collection) {
        assumeThat(collection, not(isEmptyString()));

        final KvMetadata kvMetadata1 =
                client.kv(collection, "key1")
                      .put("{}")
                      .execute();
        final KvMetadata kvMetadata2 =
                client.kv(collection, "key2")
                      .put("{}")
                      .execute();

        final KvList<String> kvList1 =
                client.list(kvMetadata1.getCollection())
                      .limit(1)
                      .get(String.class)
                      .execute();

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertNotNull(kvList1);
        assertTrue(kvList1.iterator().hasNext());

        final KvObject<String> kvObject1 = kvList1.iterator().next();
        assertNotNull(kvObject1);
        assertTrue(kvList1.hasNext());

        final KvList<String> kvList2 = kvList1.getNext().execute();
        assertNotNull(kvList2);
        assertTrue(kvList2.iterator().hasNext());

        final KvObject<String> kvObject2 = kvList2.iterator().next();
        assertNotNull(kvObject2);
    }

}
