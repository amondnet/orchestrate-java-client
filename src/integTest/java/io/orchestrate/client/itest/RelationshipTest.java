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

import com.pholser.junit.quickcheck.ForAll;
import io.orchestrate.client.*;
import org.glassfish.grizzly.utils.DataStructures;
import org.junit.Test;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.runner.RunWith;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeThat;
import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * {@link io.orchestrate.client.OrchestrateClient}.
 */
@RunWith(Theories.class)
public final class RelationshipTest extends BaseClientTest {

    @Test
    public void listRelationships() {
        final Iterable<KvObject<String>> results =
                client.relationship(collection(), "key")
                      .get(String.class, "relation")
                      .get();

        assertNull(results);
    }

    @Theory
    public void getRelationship(@ForAll(sampleSize=10) final String relation) {
        assumeThat(relation, not(isEmptyString()));

        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");

        final Boolean putResult =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                      .to(kvMetadata2.getCollection(), kvMetadata2.getKey())
                      .put(relation)
                      .get();

        final Relationship getResult =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                      .get(String.class, relation, kvMetadata2.getCollection(), kvMetadata2.getKey())
                      .get();

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertTrue(putResult);
        assertNotNull(getResult);
        assertNotNull(getResult.getRef());
    }

    @Theory
    public void getRelationshipWithProperties(@ForAll(sampleSize=10) final String relation) {
        assumeThat(relation, not(isEmptyString()));

        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");

        final ObjectNode properties = MAPPER.createObjectNode();
        properties.put("foo", "bar");

        final RelationshipMetadata putResult =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                      .to(kvMetadata2.getCollection(), kvMetadata2.getKey())
                      .put(relation, properties)
                      .get();

        final Relationship<ObjectNode> getResult =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                      .get(ObjectNode.class, relation, kvMetadata2.getCollection(), kvMetadata2.getKey())
                      .get();

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertNotNull(putResult);
        assertNotNull(getResult);
        assertNotNull(getResult.getRef());
        assertEquals(properties, getResult.getValue());
    }

    @Theory
    public void listRelationships(@ForAll(sampleSize=10) final String relation) {
        assumeThat(relation, not(isEmptyString()));

        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");

        final Boolean result =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                      .to(kvMetadata2.getCollection(), kvMetadata2.getKey())
                      .put(relation)
                      .get();

        final Iterable<KvObject<String>> results =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                      .get(String.class, relation)
                      .get();

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertTrue(result);

        final Iterator<KvObject<String>> iterator = results.iterator();
        assertTrue(iterator.hasNext());
        final KvObject<String> kvObject = iterator.next();
        assertNotNull(kvObject);
        assertEquals(kvMetadata2.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata2.getKey(), kvObject.getKey());
        assertEquals(kvMetadata2.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());
    }

    public void listRelationshipsAsync(@ForAll(sampleSize=10) final String relation)
            throws InterruptedException {
        assumeThat(relation, not(isEmptyString()));

        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");

        final Boolean result =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                        .to(kvMetadata2.getCollection(), kvMetadata2.getKey())
                        .put(relation)
                        .get();

        final BlockingQueue<Iterable> queue = DataStructures.getLTQInstance(Iterable.class);
        client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
              .get(String.class, relation)
              .on(new ResponseAdapter<RelationshipList<String>>() {
                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }

                  @Override
                  public void onSuccess(final RelationshipList<String> object) {
                      queue.add(object);
                  }
              });

        @SuppressWarnings("unchecked")
        final Iterable<KvObject<String>> results = queue.poll(5000, TimeUnit.MILLISECONDS);

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertTrue(result);

        final Iterator<KvObject<String>> iterator = results.iterator();
        assertTrue(iterator.hasNext());
        final KvObject<String> kvObject = iterator.next();
        assertNotNull(kvObject);
        assertEquals(kvMetadata2.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata2.getKey(), kvObject.getKey());
        assertEquals(kvMetadata2.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());
    }

    @Theory
    public void listRelationshipsMultiHop(@ForAll(sampleSize=10) final String relation) {
        assumeThat(relation, not(isEmptyString()));

        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");
        final KvMetadata kvMetadata3 = insertItem("key3", "{}");

        final Boolean result1 =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                        .to(kvMetadata2.getCollection(), kvMetadata2.getKey())
                        .put(relation)
                        .get();

        final Boolean result2 =
                client.relationship(kvMetadata2.getCollection(), kvMetadata2.getKey())
                      .to(kvMetadata3.getCollection(), kvMetadata3.getKey())
                      .put(relation)
                      .get();

        final Iterable<KvObject<String>> results =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                        .get(String.class, relation, relation)
                        .get();

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertNotNull(kvMetadata3);
        assertTrue(result1);
        assertTrue(result2);

        final Iterator<KvObject<String>> iterator = results.iterator();
        assertTrue(iterator.hasNext());
        final KvObject<String> kvObject = iterator.next();
        assertNotNull(kvObject);
        assertEquals(kvMetadata3.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata3.getKey(), kvObject.getKey());
        assertEquals(kvMetadata3.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());
    }

    @Theory
    public void listRelationshipsMultiHopAsync(@ForAll(sampleSize=10) final String relation)
            throws InterruptedException {
        assumeThat(relation, not(isEmptyString()));

        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");
        final KvMetadata kvMetadata3 = insertItem("key3", "{}");

        final Boolean result1 =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                        .to(kvMetadata2.getCollection(), kvMetadata2.getKey())
                        .put(relation)
                        .get();

        final Boolean result2 =
                client.relationship(kvMetadata2.getCollection(), kvMetadata2.getKey())
                        .to(kvMetadata3.getCollection(), kvMetadata3.getKey())
                        .put(relation)
                        .get();

        final BlockingQueue<Iterable> queue = DataStructures.getLTQInstance(Iterable.class);
        client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
              .get(String.class, relation, relation)
              .on(new ResponseAdapter<RelationshipList<String>>() {
                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }

                  @Override
                  public void onSuccess(final RelationshipList<String> object) {
                      queue.add(object);
                  }
              });

        @SuppressWarnings("unchecked")
        final Iterable<KvObject<String>> results = queue.poll(5000, TimeUnit.MILLISECONDS);

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertNotNull(kvMetadata3);
        assertTrue(result1);
        assertTrue(result2);

        final Iterator<KvObject<String>> iterator = results.iterator();
        assertTrue(iterator.hasNext());
        final KvObject<String> kvObject = iterator.next();
        assertNotNull(kvObject);
        assertEquals(kvMetadata3.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata3.getKey(), kvObject.getKey());
        assertEquals(kvMetadata3.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());
    }

    @Theory
    public void putRelationshipWithProperties(@ForAll(sampleSize=10) final String relation) {
        assumeThat(relation, not(isEmptyString()));

        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");

        final ObjectNode properties = MAPPER.createObjectNode();
        properties.put("foo", "bar");

        final RelationshipMetadata result =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                      .to(kvMetadata2.getCollection(), kvMetadata2.getKey())
                      .put(relation, properties)
                      .get();

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertNotNull(result);
        assertNotNull(result.getRef());
    }

    @Theory
    public void putRelationship(@ForAll(sampleSize=10) final String relation) {
        assumeThat(relation, not(isEmptyString()));

        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");

        final Boolean result =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                      .to(kvMetadata2.getCollection(), kvMetadata2.getKey())
                      .put(relation)
                      .get();

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertTrue(result);
    }

    @Theory
    public void putRelationshipAsync(@ForAll(sampleSize=10) final String relation)
            throws InterruptedException {
        assumeThat(relation, not(isEmptyString()));

        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");

        final BlockingQueue<Boolean> queue = DataStructures.getLTQInstance(Boolean.class);
        client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
              .to(kvMetadata2.getCollection(), kvMetadata2.getKey())
              .put(relation)
              .on(new ResponseAdapter<Boolean>() {
                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }

                  @Override
                  public void onSuccess(final Boolean object) {
                      queue.add(object);
                  }
              });

        final Boolean result = queue.poll(5000, TimeUnit.MILLISECONDS);

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertTrue(result);
    }

    @Theory
    public void purgeRelationship(@ForAll(sampleSize=10) final String relation) {
        assumeThat(relation, not(isEmptyString()));

        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");

        final Boolean store =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                      .to(kvMetadata2.getCollection(), kvMetadata2.getKey())
                      .put(relation)
                      .get();

        final Boolean result =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                      .to(kvMetadata2.getCollection(), kvMetadata2.getKey())
                      .purge(relation)
                      .get();

        final Iterable<KvObject<String>> check =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                      .get(String.class, relation)
                      .get();

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertTrue(store);
        assertTrue(result);
        assertFalse(check.iterator().hasNext());
    }

    @Theory
    public void purgeRelationshipAsync(@ForAll(sampleSize=10) final String relation)
            throws InterruptedException {
        assumeThat(relation, not(isEmptyString()));

        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");

        final Boolean store =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                      .to(kvMetadata2.getCollection(), kvMetadata2.getKey())
                      .put(relation)
                      .get();

        final BlockingQueue<Boolean> queue = DataStructures.getLTQInstance(Boolean.class);
        client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
              .to(kvMetadata2.getCollection(), kvMetadata2.getKey())
              .purge(relation)
              .on(new ResponseAdapter<Boolean>() {
                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }

                  @Override
                  public void onSuccess(final Boolean object) {
                      queue.add(object);
                  }
              });


        final Boolean result = queue.poll(5000, TimeUnit.MILLISECONDS);

        final Iterable<KvObject<String>> check =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                        .get(String.class, relation)
                        .get();

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertTrue(store);
        assertTrue(result);
        assertFalse(check.iterator().hasNext());
    }

    @Test
    public void getResultsAndPaginate() {
        final String collection = collection();
        final String relation = "paginate";

        final KvMetadata kvMetadata1 = insertItem("key1", "{}");
        final KvMetadata kvMetadata2 = insertItem("key2", "{}");
        final KvMetadata kvMetadata3 = insertItem("key3", "{}");

        final Boolean store1 =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                        .to(kvMetadata2.getCollection(), kvMetadata2.getKey())
                        .put(relation)
                        .get();

        final Boolean store2 =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                        .to(kvMetadata3.getCollection(), kvMetadata3.getKey())
                        .put(relation)
                        .get();

        final RelationshipList<String> results =
                client.relationship(kvMetadata1.getCollection(), kvMetadata1.getKey())
                        .limit(1)
                        .offset(0)
                        .get(String.class, relation)
                        .get();

        System.out.println(results);
        assertTrue(store1);
        assertTrue(store2);
        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertNotNull(kvMetadata3);
        assertNotNull(results);
        assertTrue(results.iterator().hasNext());

        final KvObject<String> kvObject1 = results.iterator().next();
        assertNotNull(kvObject1);
        assertTrue(results.hasNext());

        assertFalse(results.getNext().hasSent());
        final RelationshipList<String> results2 = results.getNext().get();
        System.out.println(results2);
        assertNotNull(results2);
        assertTrue(results2.iterator().hasNext());

        final KvObject<String> kvObject2 = results2.iterator().next();
        assertNotNull(kvObject2);
    }

}
