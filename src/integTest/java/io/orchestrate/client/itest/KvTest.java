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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;

/**
 * {@link io.orchestrate.client.KvResource}.
 */
@RunWith(Theories.class)
public final class KvTest {

    private static final Set<String> COLLECTIONS = new HashSet<String>();

    /** The client to run tests on. */
    private static Client client;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUpClass() {
        client = createClient();
    }

    public static Client createClient() {
        final String apiKey = "c1167b51-b0ae-45ce-b0f6-e5c57aba0a06";
        return OrchestrateClient.builder(apiKey).host("http://localhost").port(9090).useSSL(false).build();
    }

    @AfterClass
    public static void tearDownClass() throws InterruptedException {
        for (final String collection : COLLECTIONS) {
            client.deleteCollection(collection).execute();
        }
    }

    @Theory
    public void deleteKey(@ForAll(sampleSize=10) final String key) {
        assumeThat(key, not(isEmptyString()));

        final Boolean result =
                client.kv(collection(), key)
                      .delete()
                      .execute();

        assertTrue(result);
    }

    @Theory
    public void deleteKeyAsync(@ForAll(sampleSize=10) final String key)
            throws InterruptedException {
        assumeThat(key, not(isEmptyString()));

        final BlockingQueue<Boolean> queue = DataStructures.getLTQInstance(Boolean.class);
        client.kv(collection(), key)
              .delete()
              .executeAsync(new ResponseAdapter<Boolean>() {
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
        assertTrue(result);
    }

    private String collection() {
        for(StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            if(frame.getClassName().equals(KvTest.class.getName()) && !frame.getMethodName().equals("collection")) {
                String collection = frame.getMethodName();
                if(!COLLECTIONS.contains(collection)){
                    synchronized (COLLECTIONS) {
                        COLLECTIONS.add(collection);
                    }
                }
                return collection;
            }
        }
        throw new IllegalStateException("Cannot determine test method name.");
    }

    @Theory
    public void deleteKeyIfMatch(@ForAll(sampleSize=10) final String key) {
        assumeThat(key, not(isEmptyString()));

        final KvMetadata obj = client.kv(collection(), "key").put("{}").execute();

        final Boolean result =
                client.kv(obj.getCollection(), obj.getKey())
                      .ifMatch(obj.getRef())
                      .delete()
                      .execute();

        assertNotNull(obj);
        assertTrue(result);
    }

    @Theory
    public void deleteKeyIfMatchAsync(@ForAll(sampleSize=10) final String key)
            throws InterruptedException {
        assumeThat(key, not(isEmptyString()));

        final KvMetadata obj = client.kv(collection(), "key").put("{}").execute();

        final BlockingQueue<Boolean> queue = DataStructures.getLTQInstance(Boolean.class);
        client.kv(obj.getCollection(), obj.getKey())
              .ifMatch(obj.getRef())
              .delete()
              .executeAsync(new ResponseAdapter<Boolean>() {
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
        assertNotNull(obj);
        assertTrue(result);
    }

    @Theory
    public void getKey(@ForAll(sampleSize=10) final String key) {
        assumeThat(key, not(isEmptyString()));

        final KvMetadata kvMetadata =
                client.kv(collection(), key)
                      .put("{}")
                      .execute();

        final KvObject<String> object =
                client.kv(kvMetadata.getCollection(), kvMetadata.getKey())
                      .get(String.class)
                      .execute();

        assertNotNull(kvMetadata);
        assertNotNull(object);
        assertEquals(kvMetadata.getCollection(), object.getCollection());
        assertEquals(kvMetadata.getKey(), object.getKey());
        assertEquals(kvMetadata.getRef(), object.getRef());
        assertEquals("{}", object.getValue());
    }

    @Theory
    public void getKeyAsync(@ForAll(sampleSize=10) final String key)
            throws InterruptedException {
        assumeThat(key, not(isEmptyString()));

        final KvMetadata kvMetadata =
                client.kv(collection(), key)
                      .put("{}")
                      .execute();

        final BlockingQueue<KvObject> queue = DataStructures.getLTQInstance(KvObject.class);
        client.kv(kvMetadata.getCollection(), kvMetadata.getKey())
              .get(String.class)
              .executeAsync(new ResponseAdapter<KvObject<String>>() {
                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }

                  @Override
                  public void onSuccess(final KvObject<String> object) {
                      queue.add(object);
                  }
              });

        @SuppressWarnings("unchecked")
        final KvObject<String> object = queue.poll(5000, TimeUnit.MILLISECONDS);

        assertNotNull(kvMetadata);
        assertNotNull(object);
        assertEquals(kvMetadata.getCollection(), object.getCollection());
        assertEquals(kvMetadata.getKey(), object.getKey());
        assertEquals(kvMetadata.getRef(), object.getRef());
        assertEquals("{}", object.getValue());
    }

    @Theory
    public void getKeyTimed(@ForAll(sampleSize=10) final String key) {
        thrown.expect(RuntimeException.class);

        client.kv(collection(), key).get(String.class).execute(0);
    }

    @Theory
    @org.junit.Ignore
    public void getKeyWithListener(@ForAll(sampleSize=10) final String key)
            throws InterruptedException {
        assumeThat(key, not(isEmptyString()));

        final KvMetadata kvMetadata =
                client.kv(collection(), key)
                      .put("{}")
                      .execute();

        final BlockingQueue<KvObject> queue = DataStructures.getLTQInstance(KvObject.class);
        final KvObject<String> object =
                client.kv(collection(), key)
                      .get(String.class)
                      .on(new ResponseAdapter<KvObject<String>>() {
                          @Override
                          public void onFailure(final Throwable error) {
                              fail(error.getMessage());
                          }

                          @Override
                          public void onSuccess(final KvObject<String> object) {
                              queue.add(object);
                          }
                      })
                      .on(new ResponseAdapter<KvObject<String>>() {
                          @Override
                          public void onFailure(final Throwable error) {
                              fail(error.getMessage());
                          }

                          @Override
                          public void onSuccess(final KvObject<String> object) {
                              queue.add(object);
                          }
                      })
                      .execute();

        @SuppressWarnings("unchecked")
        final KvObject result1 = queue.poll(5000, TimeUnit.MILLISECONDS);
        final KvObject result2 = queue.poll(5000, TimeUnit.MILLISECONDS);

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1, result2);
    }

    @Theory
    @org.junit.Ignore
    public void getKeyWithListenerAsync(@ForAll(sampleSize=10) final String key)
            throws InterruptedException {
        final BlockingQueue<KvObject> queue = DataStructures.getLTQInstance(KvObject.class);
        client.kv(collection(), key)
              .get(String.class)
              .executeAsync(new ResponseAdapter<KvObject<String>>() {
                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }

                  @Override
                  public void onSuccess(final KvObject<String> object) {
                      queue.add(object);
                  }
              });

        @SuppressWarnings("unchecked")
        final KvObject<String> result = queue.poll(5000, TimeUnit.MILLISECONDS);
        // FIXME
    }

    @Theory
    public void purgeKey(@ForAll(sampleSize=10) final String key) {
        assumeThat(key, not(isEmptyString()));

        final KvMetadata obj = client.kv(collection(), key).put("{}").execute();

        final Boolean result =
                client.kv(obj.getCollection(), obj.getKey())
                      .delete(true)
                      .execute();

        final KvObject<String> nullObj =
                client.kv(obj.getCollection(), obj.getKey())
                      .get(String.class, obj.getRef())
                      .execute();

        assertNotNull(obj);
        assertTrue(result);
        assertNull(nullObj);
    }

    @Theory
    public void purgeKeyAsync(@ForAll(sampleSize=10) final String key)
            throws InterruptedException {
        assumeThat(key, not(isEmptyString()));

        final KvMetadata obj = client.kv(collection(), key).put("{}").execute();

        final BlockingQueue<Boolean> queue = DataStructures.getLTQInstance(Boolean.class);
        client.kv(obj.getCollection(), obj.getKey())
              .delete(Boolean.TRUE)
              .executeAsync(new ResponseAdapter<Boolean>() {
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

        final KvObject<String> nullObj =
                client.kv(obj.getCollection(), obj.getKey())
                      .get(String.class, obj.getRef())
                      .execute();

        assertNotNull(obj);
        assertTrue(result);
        assertNull(nullObj);
    }

    @Theory
    public void purgeKeyIfMatch(@ForAll(sampleSize=10) final String key) {
        assumeThat(key, not(isEmptyString()));

        final KvMetadata obj = client.kv(collection(), key).put("{}").execute();

        final Boolean result =
                client.kv(obj.getCollection(), obj.getKey())
                      .ifMatch(obj.getRef())
                      .delete(Boolean.TRUE)
                      .execute();

        final KvObject<String> nullObj =
                client.kv(obj.getCollection(), obj.getKey())
                      .get(String.class, obj.getRef())
                      .execute();

        assertNotNull(obj);
        assertTrue(result);
        assertNull(nullObj);
    }

    @Theory
    public void purgeKeyIfMatchAsync(@ForAll(sampleSize=10) final String key)
            throws InterruptedException {
        assumeThat(key, not(isEmptyString()));

        final KvMetadata obj = client.kv(collection(), key).put("{}").execute();

        final BlockingQueue<Boolean> queue = DataStructures.getLTQInstance(Boolean.class);
        client.kv(obj.getCollection(), obj.getKey())
              .ifMatch(obj.getRef())
              .delete(Boolean.TRUE)
              .executeAsync(new ResponseAdapter<Boolean>() {
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

        final KvObject<String> nullObj =
                client.kv(obj.getCollection(), obj.getKey())
                      .get(String.class, obj.getRef())
                      .execute();

        assertNotNull(obj);
        assertTrue(result);
        assertNull(nullObj);
    }

    @Theory
    public void putKey(@ForAll(sampleSize=10) final String key) {
        assumeThat(key, not(isEmptyString()));

        final KvMetadata kvMetadata =
                client.kv(collection(), key)
                      .put("{}")
                      .execute();

        final KvObject<String> kvObject =
                client.kv(kvMetadata.getCollection(), kvMetadata.getKey())
                      .get(String.class)
                      .execute();

        assertNotNull(kvMetadata);
        assertNotNull(kvObject);
        assertEquals(kvMetadata.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata.getKey(), kvObject.getKey());
        assertEquals(kvMetadata.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());
    }

    @Theory
    public void putKeyAsync(@ForAll(sampleSize=10) final String key)
            throws InterruptedException {
        assumeThat(key, not(isEmptyString()));

        final BlockingQueue<KvMetadata> queue = DataStructures.getLTQInstance(KvMetadata.class);
        client.kv(collection(), key)
              .put("{}")
              .executeAsync(new ResponseAdapter<KvMetadata>() {
                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }

                  @Override
                  public void onSuccess(final KvMetadata object) {
                      queue.add(object);
                  }
              });

        final KvMetadata kvMetadata = queue.poll(5000, TimeUnit.MILLISECONDS);

        final KvObject<String> kvObject =
                client.kv(kvMetadata.getCollection(), kvMetadata.getKey())
                        .get(String.class)
                        .execute();

        assertNotNull(kvMetadata);
        assertNotNull(kvObject);
        assertEquals(kvMetadata.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata.getKey(), kvObject.getKey());
        assertEquals(kvMetadata.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());
    }

    @Theory
    public void putKeyIfAbsent(@ForAll(sampleSize=10) final String key) {
        assumeThat(key, not(isEmptyString()));

        final KvMetadata kvMetadata =
                client.kv(collection(), key)
                      .ifAbsent(Boolean.TRUE)
                      .put("{}")
                      .execute();

        final KvObject<String> kvObject =
                client.kv(kvMetadata.getCollection(), kvMetadata.getKey())
                        .get(String.class)
                        .execute();

        assertNotNull(kvMetadata);
        assertNotNull(kvObject);
        assertEquals(kvMetadata.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata.getKey(), kvObject.getKey());
        assertEquals(kvMetadata.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());

        final KvMetadata kvMetadata2 =
                client.kv(kvMetadata.getCollection(), kvMetadata.getKey())
                      .ifAbsent(Boolean.TRUE)
                      .put("{}")
                      .execute();

        assertNull(kvMetadata2);
    }

    @Theory
    public void putKeyIfAbsentAsync(@ForAll(sampleSize=10) final String key)
            throws InterruptedException {
        assumeThat(key, not(isEmptyString()));

        final BlockingQueue<KvMetadata> queue = DataStructures.getLTQInstance(KvMetadata.class);
        client.kv(collection(), key)
              .ifAbsent(Boolean.TRUE)
              .put("{}")
              .executeAsync(new ResponseAdapter<KvMetadata>() {
                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }

                  @Override
                  public void onSuccess(final KvMetadata object) {
                      queue.add(object);
                  }
              });

        final KvMetadata kvMetadata = queue.poll(5000, TimeUnit.MILLISECONDS);

        final KvObject<String> kvObject =
                client.kv(kvMetadata.getCollection(), kvMetadata.getKey())
                      .get(String.class)
                      .execute();

        assertNotNull(kvMetadata);
        assertNotNull(kvObject);
        assertEquals(kvMetadata.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata.getKey(), kvObject.getKey());
        assertEquals(kvMetadata.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());

        final KvMetadata kvMetadata2 =
                client.kv(kvMetadata.getCollection(), kvMetadata.getKey())
                      .ifAbsent(Boolean.TRUE)
                      .put("{}")
                      .execute();

        assertNull(kvMetadata2);
    }

    @Theory
    public void putKeyIfMatch(@ForAll(sampleSize=10) final String key) {
        assumeThat(key, not(isEmptyString()));

        final String collection = collection();
        final KvMetadata kvMetadata =
                client.kv(collection, key)
                      .put("{}")
                      .execute();

        final KvMetadata kvMetadata2 =
                client.kv(kvMetadata.getCollection(), kvMetadata.getKey())
                      .ifMatch(kvMetadata.getRef())
                      .put("{}")
                      .execute();

        assertNotNull(kvMetadata);
        assertNotNull(kvMetadata2);
        assertEquals(collection, kvMetadata2.getCollection());
        assertEquals(key, kvMetadata2.getKey());
    }

    @Theory
    public void putKeyIfMatchAsync(@ForAll(sampleSize=10) final String key)
            throws InterruptedException {
        assumeThat(key, not(isEmptyString()));

        final String collection = collection();
        final KvMetadata kvMetadata =
                client.kv(collection, key)
                      .put("{}")
                      .execute();

        final BlockingQueue<KvMetadata> queue = DataStructures.getLTQInstance(KvMetadata.class);
        client.kv(kvMetadata.getCollection(), kvMetadata.getKey())
              .ifMatch(kvMetadata.getRef())
              .put("{}")
              .executeAsync(new ResponseAdapter<KvMetadata>() {
                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }

                  @Override
                  public void onSuccess(final KvMetadata object) {
                      queue.add(object);
                  }
              });

        final KvMetadata kvMetadata2 = queue.poll(5000, TimeUnit.MILLISECONDS);

        assertNotNull(kvMetadata);
        assertNotNull(kvMetadata2);
        assertEquals(collection, kvMetadata2.getCollection());
        assertEquals(key, kvMetadata2.getKey());
    }

}
