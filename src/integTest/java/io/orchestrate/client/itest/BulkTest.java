package io.orchestrate.client.itest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.orchestrate.client.*;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class BulkTest extends BaseClientTest {

    @Test
    public void insertTwoItems() throws IOException {
        User user1 = new User("user1", "user1 description");
        User user2 = new User("user2", "user2 description");

        BulkResponse response = client.bulk()
                .add(client.kv(collection(), "user1").bulkPut(user1))
                .add(client.kv(collection(), "user2").bulkPut(user2))
                .done()
                .get();

        KvList<User> items = client.listCollection(collection())
                .get(User.class)
                .get();

        // Assert items

        assertEquals(2, items.getCount());
        final Iterator<KvObject<User>> itemsIterator = items.iterator();
        final KvObject<User> user1Item = itemsIterator.next();
        assertUser(collection(), "user1", user1, user1Item);
        final KvObject<User> user2Item = itemsIterator.next();
        assertUser(collection(), "user2", user2, user2Item);

        // Assert response

        assertNotNull(response);
        assertEquals(BulkStatus.SUCCESS, response.getStatus());
        assertEquals(2, response.getSuccessCount());
        assertEquals(2, response.getResults().size());
        final Iterator<BulkResult> resultsIterator = response.getResults().iterator();
        assertBulkSuccessResult(BulkResultStatus.SUCCESS, 0, user1Item, (BulkSuccessResult) resultsIterator.next());
        assertBulkSuccessResult(BulkResultStatus.SUCCESS, 1, user2Item, (BulkSuccessResult) resultsIterator.next());
    }

    @Test
    public void bulkOperation_supportJsonString() throws IOException {
        BulkResponse response = client.bulk()
                .add(client.kv(collection(), "user1").bulkPut("{}"))
                .done()
                .get();

        assertNotNull(response);
        assertEquals(BulkStatus.SUCCESS, response.getStatus());
    }

    @Test
    public void insertTwoItems_whenOneFails_ReturnsPartialSuccessWithError() throws IOException {
        User user1 = new User("user1", "user1 description");

        BulkResponse response = client.bulk()
                .add(client.kv(collection(), "user1").bulkPut(user1))
                .add(new BulkOperation())
                .done()
                .get();

        KvList<User> items = client.listCollection(collection())
                .get(User.class)
                .get();

        // Assert items

        assertEquals(1, items.getCount());
        final Iterator<KvObject<User>> itemsIterator = items.iterator();
        final KvObject<User> user1Item = itemsIterator.next();
        assertUser(collection(), "user1", user1, user1Item);

        // Assert response

        assertNotNull(response);
        assertEquals(BulkStatus.PARTIAL, response.getStatus());
        assertEquals(1, response.getSuccessCount());
        assertEquals(2, response.getResults().size());
        final Iterator<BulkResult> resultsIterator = response.getResults().iterator();
        assertBulkSuccessResult(BulkResultStatus.SUCCESS, 0, user1Item, (BulkSuccessResult) resultsIterator.next());
        assertBulkFailureResult_FromEmptyBulkOperation((BulkFailureResult) resultsIterator.next(), 1);
    }

    @Test
    public void insertTwoItems_whenBothFails_ReturnsFailureWithErrors() throws IOException {
        BulkResponse response = client.bulk()
                .add(new BulkOperation())
                .add(new BulkOperation())
                .done()
                .get();

        assertNotNull(response);
        assertEquals(BulkStatus.FAILURE, response.getStatus());
        assertEquals(0, response.getSuccessCount());
        assertEquals(2, response.getResults().size());
        final Iterator<BulkResult> resultsIterator = response.getResults().iterator();
        assertBulkFailureResult_FromEmptyBulkOperation((BulkFailureResult) resultsIterator.next(), 0);
        assertBulkFailureResult_FromEmptyBulkOperation((BulkFailureResult) resultsIterator.next(), 1);
    }

    @Test
    public void insertItemWithTwoEvents() throws IOException {
        User user1 = new User("user1", "user1 description");
        Long now = System.currentTimeMillis();
        Long then = now - 3000;

        BulkResponse response = client.bulk()
                .add(client.kv(collection(), "user1").bulkPut(user1))
                .add(client.event(collection(), "user1").type("blinked").timestamp(then).bulkCreate(new BlinkData(1)))
                .add(client.event(collection(), "user1").type("blinked").timestamp(now).bulkCreate(new BlinkData(2)))
                .done()
                .get();

        KvList<User> items = client.listCollection(collection())
                .get(User.class)
                .get();

        // Assert items

        assertEquals(1, items.getCount());
        final Iterator<KvObject<User>> itemsIterator = items.iterator();
        KvObject<User> user1Item = itemsIterator.next();
        assertUser(collection(), "user1", user1, user1Item);

        // Assert events

        final EventList<BlinkData> events = client.event(collection(), "user1")
                .type("blinked")
                .get(BlinkData.class)
                .get();

        ArrayList<Event<BlinkData>> eventList = new ArrayList<Event<BlinkData>>();
        for (Event<BlinkData> event : events.getEvents()) {
            eventList.add(event);
        }

        assertEquals(2, eventList.size());
        assertBlinkEvent(collection(), "user1", "blinked", now, 2, eventList.get(0));
        assertBlinkEvent(collection(), "user1", "blinked", then, 1, eventList.get(1));

        // Assert response

        assertNotNull(response);
        assertEquals(BulkStatus.SUCCESS, response.getStatus());
        assertEquals(3, response.getSuccessCount());
        assertEquals(3, response.getResults().size());
        final Iterator<BulkResult> resultsIterator = response.getResults().iterator();
        assertBulkSuccessResult(BulkResultStatus.SUCCESS, 0, user1Item, (BulkSuccessResult) resultsIterator.next());
        assertBulkSuccessResult(BulkResultStatus.SUCCESS, 1, eventList.get(1), (BulkSuccessResult) resultsIterator.next());
        assertBulkSuccessResult(BulkResultStatus.SUCCESS, 2, eventList.get(0), (BulkSuccessResult) resultsIterator.next());
    }

    @Test
    public void insertTwoItemsWithTwoRelationships() throws IOException {
        User user1 = new User("user1", "user1 description");
        User user2 = new User("user2", "user2 description");
        JsonNode friendProperties = new ObjectMapper().readTree("{ \"bestFriend\" : true }");

        BulkResponse response = client.bulk()
                .add(client.kv(collection(), "user1").bulkPut(user1))
                .add(client.kv(collection(), "user2").bulkPut(user2))
                .add(client.relationship(collection(), "user1").to(collection(), "user2").bulkPut("friends"))
                .add(client.relationship(collection(), "user2").to(collection(), "user1").bulkPut("friends", friendProperties))
                .done()
                .get();

        // Assert items

        KvList<User> items = client.listCollection(collection())
                .get(User.class)
                .get();

        assertEquals(2, items.getCount());
        final Iterator<KvObject<User>> itemsIterator = items.iterator();
        final KvObject<User> user1Item = itemsIterator.next();
        assertUser(collection(), "user1", user1, user1Item);
        final KvObject<User> user2Item = itemsIterator.next();
        assertUser(collection(), "user2", user2, user2Item);

        // Assert user1 relationship

        Relationship<JsonNode> user1Friend = client.relationship(collection(), "user1")
                .get(JsonNode.class, "friends", collection(), "user2")
                .get();

        assertEquals("user2", user1Friend.getDestinationKey());

        // Assert user2 relationship

        Relationship<JsonNode> user2Friend = client.relationship(collection(), "user2")
                .get(JsonNode.class, "friends", collection(), "user1")
                .get();

        assertEquals("user1", user2Friend.getDestinationKey());
        assertEquals(friendProperties, user2Friend.getValue());
    }

    @Test
    public void whenAddIsCalledAfterDone_throw() throws IOException {
        BulkResource bulkResource = client.bulk();
        bulkResource.add(client.kv(collection(), "item1").bulkPut("{}"));
        bulkResource.done();

        Throwable actualException = null;

        try {
            bulkResource.add(client.kv(collection(), "item2").bulkPut("{}"));
        } catch (Throwable ex) {
            actualException = ex;
        }

        assertNotNull(actualException);
        assertEquals("Can not add an operation after calling 'done'", actualException.getMessage());
    }

    private void assertBlinkEvent(
            String expectedCollection,
            String expectedKey,
            String expectedType,
            Long expectedTimestamp,
            int expectedBlinkSpeed,
            Event<BlinkData> actual) {
        assertEquals(expectedCollection, actual.getCollection());
        assertEquals(expectedKey, actual.getKey());
        assertEquals(expectedType, actual.getType());
        assertEquals(expectedTimestamp, actual.getTimestamp());
        assertEquals(expectedBlinkSpeed, actual.getValue().speed);
    }

    private void assertBulkFailureResult_FromEmptyBulkOperation(BulkFailureResult errorResult, int expectedOperationIndex) {
        assertEquals(expectedOperationIndex, errorResult.getOperationIndex());
        assertEquals(BulkResultStatus.FAILURE, errorResult.getStatus());
        assertEquals("The API request is malformed.", errorResult.getError().getMessage());
        assertEquals("api_bad_request", errorResult.getError().getCode());
        assertEquals("Can't read bulk operation from malformed JSON",
                errorResult.getError().getDetails().get("info"));
    }

    private void assertBulkSuccessResult(
            BulkResultStatus expectedStatus,
            int expectedOperationIndex,
            KvObject expectedItem,
            BulkSuccessResult actual
    ) {

        assertEquals(expectedStatus, actual.getStatus());
        assertEquals(expectedOperationIndex, actual.getOperationIndex());
        assertItemPath(expectedItem, actual.getItemPath());

        if (actual.getItemPath().getKind() == ItemKind.EVENT) {
            Event expectedEvent = (Event) expectedItem;
            EventPath actualEvent = ((BulkSuccessResult<EventPath>) actual).getItemPath();
            assertEquals(expectedEvent.getOrdinal(), actualEvent.getOrdinal());
            assertEquals(expectedEvent.getType(), actualEvent.getType());
            assertEquals(expectedEvent.getTimestamp(), actualEvent.getTimestamp());
        }
    }

    private void assertItemPath(KvObject expectedItem, ItemPath actual) {
        assertEquals(expectedItem.getCollection(), actual.getCollection());
        assertEquals(expectedItem.getKey(), actual.getKey());
        assertEquals(expectedItem.getRef(), actual.getRef());
        assertEquals(expectedItem.getReftime(), actual.getReftime());
    }

    private void assertUser(
            String expectedCollection,
            String expectedKey,
            User expectedUser,
            KvObject<User> actual) {

        assertEquals(expectedCollection, actual.getCollection());
        assertEquals(expectedKey, actual.getKey());
        assertEquals(expectedUser, actual.getValue());
    }
}
