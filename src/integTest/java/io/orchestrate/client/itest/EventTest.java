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
import io.orchestrate.client.jsonpatch.JsonPatch;
import org.glassfish.grizzly.utils.DataStructures;
import org.junit.Test;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeThat;

/**
 * {@link io.orchestrate.client.OrchestrateClient#event(String, String)}.
 */
@RunWith(Theories.class)
public final class EventTest extends BaseClientTest {

    @Theory
    public void putEvent(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final EventMetadata eventMetadata =
                client.event(collection(), "key")
                      .type(type)
                      .create("{}")
                      .get();

        assertNotNull(eventMetadata);
    }

    @Theory
    public void putEventAsync(@ForAll(sampleSize=10) final String type)
            throws InterruptedException {
        assumeThat(type, not(isEmptyString()));

        final BlockingQueue<EventMetadata> queue = DataStructures.getLTQInstance(EventMetadata.class);
        client.event(collection(), "key")
              .type(type)
              .create("{}")
              .on(new ResponseAdapter<EventMetadata>() {
                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }

                  @Override
                  public void onSuccess(final EventMetadata object) {
                      queue.add(object);
                  }
              });

        final EventMetadata result = queue.poll(5000, TimeUnit.MILLISECONDS);
        assertNotNull(result);
    }

    @Theory
    public void putEventWithTimestamp(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final Boolean result =
                client.event(collection(), "key")
                      .type(type)
                      .put("{}", System.currentTimeMillis())
                      .get();

        assertTrue(result);
    }

    @Theory
    public void putEventWithTimestampAsync(@ForAll(sampleSize=10) final String type)
            throws InterruptedException {
        assumeThat(type, not(isEmptyString()));

        final BlockingQueue<Boolean> queue = DataStructures.getLTQInstance(Boolean.class);
        client.event(collection(), "key")
              .type(type)
              .put("{}", System.currentTimeMillis())
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
        assertTrue(result);
    }

    @Theory
    public void createEvent(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final EventMetadata eventMeta =
            client.event(collection(), "key")
                .type(type)
                .create("{}")
                .get();

        assertNotNull(eventMeta);
        assertNotNull(eventMeta.getTimestamp());
    }

    private static final Random RAND = new Random();
    @Theory
    public void getSingleEvent(@ForAll(sampleSize=10) final String badType) {
        // not using the provided badType b/c it is allowing illegal (reserved) characters
        // which is causing the comparison of the 'type' to fail. in later tests, we do not
        // compare on the event type, so we do use the provided values.
        String key = Long.toHexString(RAND.nextLong());
        String type = Long.toHexString(RAND.nextLong());

        final EventMetadata eventMeta =
                client.event(collection(), key)
                        .type(type)
                        .create("{}")
                        .get();

        final EventList<String> eventList = client.event(collection(), key)
                .type(type)
                .get(String.class)
                .get();

        Event foundEvent = null;
        int count =0;
        for(Event<String> event : eventList.getEvents()) {
            ++count;
            foundEvent = event;
        }
        assertEquals(1, count);

        assertNotNull(foundEvent);
        assertEquals("{}", foundEvent.getValue());
        assertEquals("{}", foundEvent.getRawValue());
        assertEquals(collection(), foundEvent.getCollection());
        assertEquals(key, foundEvent.getKey());
        assertEquals(type, foundEvent.getType());
        assertEquals(eventMeta.getTimestamp(), foundEvent.getTimestamp());
        assertEquals(eventMeta.getOrdinal(), foundEvent.getOrdinal());
        assertEquals(eventMeta.getRef(), foundEvent.getRef());

        final Event<String> event = client.event(collection(), key)
                .type(type)
                .timestamp(eventMeta.getTimestamp())
                .ordinal(eventMeta.getOrdinal())
                .get(String.class)
                .get();

        assertNotNull(event);
        assertEquals("{}", event.getValue());
        assertEquals("{}", event.getRawValue());
        assertEquals(collection(), event.getCollection());
        assertEquals(key, event.getKey());
        assertEquals(type, event.getType());
        assertEquals(eventMeta.getTimestamp(), event.getTimestamp());
        assertEquals(eventMeta.getOrdinal(), event.getOrdinal());
        assertEquals(eventMeta.getRef(), event.getRef());
    }

    @Theory
    public void purgeSingleEvent(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final EventMetadata eventMeta =
                client.event(collection(), "key")
                        .type(type)
                        .create("{}")
                        .get();

        final Boolean purged = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMeta.getTimestamp())
                .ordinal(eventMeta.getOrdinal())
                .purge()
                .get();

        assertTrue(purged);

        final Event<String> event = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMeta.getTimestamp())
                .ordinal(eventMeta.getOrdinal())
                .get(String.class)
                .get();

        assertNull(event);
    }

    @Theory
    public void conditionalPurgeSingleEvent(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final EventMetadata eventMeta =
                client.event(collection(), "key")
                        .type(type)
                        .create("{}")
                        .get();

        final Boolean purged = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMeta.getTimestamp())
                .ordinal(eventMeta.getOrdinal())
                .ifMatch(eventMeta.getRef())
                .purge()
                .get();

        assertTrue(purged);

        final Event<String> event = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMeta.getTimestamp())
                .ordinal(eventMeta.getOrdinal())
                .get(String.class)
                .get();

        assertNull(event);
    }

    @Theory
    public void conditionalPurgeSingleEventMismatch(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final EventMetadata eventMeta =
                client.event(collection(), "key")
                        .type(type)
                        .create("{}")
                        .get();

        String badRef = "aa50cde812389420";

        ItemVersionMismatchException mismatchEx = null;

        try {
            final Boolean purged = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMeta.getTimestamp())
                .ordinal(eventMeta.getOrdinal())
                .ifMatch(badRef)
                .purge()
                .get();
        } catch (ItemVersionMismatchException ex) {
            mismatchEx = ex;
        }

        assertNotNull(mismatchEx);

        final Event<String> event = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMeta.getTimestamp())
                .ordinal(eventMeta.getOrdinal())
                .get(String.class)
                .get();

        assertNotNull(event);
        assertEquals(eventMeta.getRef(), event.getRef());
    }

    @Theory
    public void updateSingleEvent(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final EventMetadata eventMetaV1 =
                client.event(collection(), "key")
                        .type(type)
                        .create("{}")
                        .get();

        final EventMetadata eventMetaV2 =
                client.event(collection(), "key")
                        .type(type)
                        .timestamp(eventMetaV1.getTimestamp())
                        .ordinal(eventMetaV1.getOrdinal())
                        .update("{\"name\":\"test\"}")
                        .get();

        final Event<String> event = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMetaV1.getTimestamp())
                .ordinal(eventMetaV1.getOrdinal())
                .get(String.class)
                .get();

        assertEquals(eventMetaV2.getRef(), event.getRef());
    }

    @Theory
    public void conditionalUpdateSingleEvent(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final EventMetadata eventMetaV1 =
                client.event(collection(), "key")
                        .type(type)
                        .create("{}")
                        .get();

        final EventMetadata eventMetaV2 =
                client.event(collection(), "key")
                        .type(type)
                        .timestamp(eventMetaV1.getTimestamp())
                        .ordinal(eventMetaV1.getOrdinal())
                        .ifMatch(eventMetaV1.getRef())
                        .update("{\"name\":\"test\"}")
                        .get();

        final Event<String> event = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMetaV1.getTimestamp())
                .ordinal(eventMetaV1.getOrdinal())
                .get(String.class)
                .get();

        assertEquals(eventMetaV2.getRef(), event.getRef());
    }

    @Theory
    public void conditionalUpdateSingleEventMismatch(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final EventMetadata eventMetaV1 =
                client.event(collection(), "key")
                        .type(type)
                        .create("{}")
                        .get();

        String badRef = "aa50cde812389420";

        ItemVersionMismatchException mismatchEx = null;
        try {
            final EventMetadata eventMetaV2 =
                client.event(collection(), "key")
                        .type(type)
                        .timestamp(eventMetaV1.getTimestamp())
                        .ordinal(eventMetaV1.getOrdinal())
                        .ifMatch(badRef)
                        .update("{\"name\":\"test\"}")
                        .get();
        } catch (ItemVersionMismatchException ex) {
            mismatchEx = ex;
        }

        assertNotNull(mismatchEx);

        final Event<String> event = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMetaV1.getTimestamp())
                .ordinal(eventMetaV1.getOrdinal())
                .get(String.class)
                .get();

        assertEquals(eventMetaV1.getRef(), event.getRef());
    }

    @Theory
    public void patchSingleEvent(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final EventMetadata eventMetaV1 =
                client.event(collection(), "key")
                        .type(type)
                        .create("{}")
                        .get();

        final EventMetadata eventMetaV2 =
                client.event(collection(), "key")
                        .type(type)
                        .timestamp(eventMetaV1.getTimestamp())
                        .ordinal(eventMetaV1.getOrdinal())
                        .patch(JsonPatch.builder()
                            .add("name", "test")
                            .build()
                        )
                        .get();

        final Event<String> event = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMetaV1.getTimestamp())
                .ordinal(eventMetaV1.getOrdinal())
                .get(String.class)
                .get();

        assertEquals(eventMetaV2.getRef(), event.getRef());
        assertEquals("{\"name\":\"test\"}", event.getValue());
        assertEquals("{\"name\":\"test\"}", event.getRawValue());
    }

    @Theory
    public void conditionalPatchSingleEvent(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final EventMetadata eventMetaV1 =
                client.event(collection(), "key")
                        .type(type)
                        .create("{}")
                        .get();

        final EventMetadata eventMetaV2 =
                client.event(collection(), "key")
                        .type(type)
                        .timestamp(eventMetaV1.getTimestamp())
                        .ordinal(eventMetaV1.getOrdinal())
                        .ifMatch(eventMetaV1.getRef())
                        .patch(JsonPatch.builder()
                                .add("name", "test")
                                .build()
                        )
                        .get();

        final Event<String> event = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMetaV1.getTimestamp())
                .ordinal(eventMetaV1.getOrdinal())
                .get(String.class)
                .get();

        assertEquals(eventMetaV2.getRef(), event.getRef());
        assertEquals("{\"name\":\"test\"}", event.getValue());
    }

    @Theory
    public void conditionalPatchSingleEventMismatch(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final EventMetadata eventMetaV1 =
                client.event(collection(), "key")
                        .type(type)
                        .create("{}")
                        .get();

        String badRef = "aa50cde812389420";

        ItemVersionMismatchException mismatchEx = null;
        try {
            final EventMetadata eventMetaV2 =
                client.event(collection(), "key")
                        .type(type)
                        .timestamp(eventMetaV1.getTimestamp())
                        .ordinal(eventMetaV1.getOrdinal())
                        .ifMatch(badRef)
                        .patch(JsonPatch.builder()
                                .add("name", "test")
                                .build()
                        )
                        .get();
        } catch (ItemVersionMismatchException ex) {
            mismatchEx = ex;
        }

        assertNotNull(mismatchEx);


        final Event<String> event = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMetaV1.getTimestamp())
                .ordinal(eventMetaV1.getOrdinal())
                .get(String.class)
                .get();

        assertEquals(eventMetaV1.getRef(), event.getRef());
        assertEquals("{}", event.getValue());
    }

    @Theory
    public void mergePatchSingleEvent(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final EventMetadata eventMetaV1 =
                client.event(collection(), "key")
                        .type(type)
                        .create("{}")
                        .get();

        final EventMetadata eventMetaV2 =
                client.event(collection(), "key")
                        .type(type)
                        .timestamp(eventMetaV1.getTimestamp())
                        .ordinal(eventMetaV1.getOrdinal())
                        .merge("{\"name\":\"test\"}")
                        .get();

        final Event<String> event = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMetaV1.getTimestamp())
                .ordinal(eventMetaV1.getOrdinal())
                .get(String.class)
                .get();

        assertEquals(eventMetaV2.getRef(), event.getRef());
        assertEquals("{\"name\":\"test\"}", event.getValue());
        assertEquals("{\"name\":\"test\"}", event.getRawValue());
    }

    @Theory
    public void conditionalMergePatchSingleEvent(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final EventMetadata eventMetaV1 =
                client.event(collection(), "key")
                        .type(type)
                        .create("{}")
                        .get();

        final EventMetadata eventMetaV2 =
                client.event(collection(), "key")
                        .type(type)
                        .timestamp(eventMetaV1.getTimestamp())
                        .ordinal(eventMetaV1.getOrdinal())
                        .ifMatch(eventMetaV1.getRef())
                        .merge("{\"name\":\"test\"}")
                        .get();

        final Event<String> event = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMetaV1.getTimestamp())
                .ordinal(eventMetaV1.getOrdinal())
                .get(String.class)
                .get();

        assertEquals(eventMetaV2.getRef(), event.getRef());
        assertEquals("{\"name\":\"test\"}", event.getValue());
        assertEquals("{\"name\":\"test\"}", event.getRawValue());
    }

    @Theory
    public void conditionalMergePatchSingleEventMismatch(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final EventMetadata eventMetaV1 =
                client.event(collection(), "key")
                        .type(type)
                        .create("{}")
                        .get();

        String badRef = "aa50cde812389420";

        ItemVersionMismatchException mismatchEx = null;
        try {
            final EventMetadata eventMetaV2 =
                    client.event(collection(), "key")
                            .type(type)
                            .timestamp(eventMetaV1.getTimestamp())
                            .ordinal(eventMetaV1.getOrdinal())
                            .ifMatch(badRef)
                            .merge("{\"name\":\"test\"}")
                            .get();
        } catch (ItemVersionMismatchException ex) {
            mismatchEx = ex;
        }

        assertNotNull(mismatchEx);


        final Event<String> event = client.event(collection(), "key")
                .type(type)
                .timestamp(eventMetaV1.getTimestamp())
                .ordinal(eventMetaV1.getOrdinal())
                .get(String.class)
                .get();

        assertEquals(eventMetaV1.getRef(), event.getRef());
        assertEquals("{}", event.getValue());
    }

    @Test
    public void listEventsAndApplyWhitelistFieldFiltering() throws InterruptedException, IOException {

        String key = Long.toHexString(RAND.nextLong());
        EventMetadata eventMetadata = client.event(collection(), key)
            .type("type")
            .create("{`foo`:`bar`,`bing`:`bong`,`zip`:`zap`}".replace('`', '"'))
            .get();

        EventList<JsonNode> eventList = client.event(collection(), key)
            .withFields("value.foo")
            .type("type")
            .get(JsonNode.class)
            .get();

        assertNotNull(eventMetadata);
        assertNotNull(eventList);
        assertTrue(eventList.iterator().hasNext());

        Event<JsonNode> eventObject = eventList.iterator().next();

        JsonNode resultJson = eventObject.getValue();
        assertEquals("bar", resultJson.get("foo").asText());
        assertFalse(resultJson.has("bing"));
        assertFalse(resultJson.has("zip"));
    }

    @Test
    public void listEventsAndApplyBlacklistFieldFiltering() throws InterruptedException, IOException {

        String key = Long.toHexString(RAND.nextLong());
        EventMetadata eventMetadata = client.event(collection(), key)
            .type("type")
            .create("{`foo`:`bar`,`bing`:`bong`,`zip`:`zap`}".replace('`', '"'))
            .get();

        EventList<JsonNode> eventList = client.event(collection(), key)
            .withoutFields("value.foo")
            .type("type")
            .get(JsonNode.class)
            .get();

        assertNotNull(eventMetadata);
        assertNotNull(eventList);
        assertTrue(eventList.iterator().hasNext());

        Event<JsonNode> eventObject = eventList.iterator().next();

        JsonNode resultJson = eventObject.getValue();
        assertEquals("bong", resultJson.get("bing").asText());
        assertEquals("zap", resultJson.get("zip").asText());
        assertFalse(resultJson.has("foo"));
    }
}
