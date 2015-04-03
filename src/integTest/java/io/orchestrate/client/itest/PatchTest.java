package io.orchestrate.client.itest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.orchestrate.client.*;
import io.orchestrate.client.jsonpatch.JsonPatch;
import io.orchestrate.client.jsonpatch.JsonPatchOp;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PatchTest extends BaseClientTest {

    private String key;

    @Before
    public void setup() {
        key = Long.toHexString(RAND.nextLong());
    }

    @Test
    public void withTestOp() {
        insertItem(key, "{`name`:`Test1`}");

        final KvMetadata patched = client.kv(collection(), key)
            .patch(
                JsonPatch.builder()
                    .test("name", "Test1")
                    .add("name", "Test2")
                    .build()
            )
            .get();

        final KvObject<ObjectNode> kvObject = readItem(key);

        assertEquals("Test2", kvObject.getValue().get("name").textValue());
    }

    @Test
    public void withTestOpFailure() {
        insertItem(key, "{`name`:`Test1`}");

        TestOpApplyException thrown = null;
        try {
            final KvMetadata patched = client.kv(collection(), key)
                .patch(
                    JsonPatch.builder()
                        .test("name", "Test2") // should fail b/c we initialized name to Test1
                        .add("name", "Test3")
                        .build()
                )
                .get();
            fail("Should have failed with a concurrency conflict.");
        } catch (TestOpApplyException ex) {
            thrown = ex;
        }

        assertNotNull(thrown);
        assertEquals(0, (int)thrown.getOpIndex());
        assertEquals("test", thrown.getOp().getOp());
        assertNotNull(thrown.getDetails().get("expected"));

        final KvObject<ObjectNode> kvObject = readItem(key);

        // should not have changed name b/c test op failed.
        assertEquals("Test1", kvObject.getValue().get("name").textValue());
    }

    @Test
    public void withTestOpNegated() {
        insertItem(key, "{`name`:`Test1`}");

        final KvMetadata patched = client.kv(collection(), key)
                .patch(
                        JsonPatch.builder()
                                .testNot("name", "Test2")
                                .add("name", "Test2")
                                .build()
                )
                .get();

        final KvObject<ObjectNode> kvObject = readItem(key);

        assertEquals("Test2", kvObject.getValue().get("name").textValue());
    }

    @Test
    public void withTestOpFieldPresent() {
        insertItem(key, "{`name`:`Test1`}");

        final KvMetadata patched = client.kv(collection(), key)
                .patch(
                        JsonPatch.builder()
                                .testFieldPresent("name")
                                .add("name", "Test2")
                                .build()
                )
                .get();

        final KvObject<ObjectNode> kvObject = readItem(key);

        assertEquals("Test2", kvObject.getValue().get("name").textValue());
    }

    @Test
    public void withTestOpFieldMissing() {
        insertItem(key, "{`name`:`Test1`}");

        final KvMetadata patched = client.kv(collection(), key)
                .patch(
                        JsonPatch.builder()
                                .testFieldMissing("name2")
                                .add("name2", "Test2")
                                .build()
                )
                .get();

        final KvObject<ObjectNode> kvObject = readItem(key);

        assertEquals("Test2", kvObject.getValue().get("name2").textValue());
    }

    @Test
    public void withOpFailureMissingPath() {
        KvMetadata orig = insertItem(key, "{}");

        PatchConflictException thrown = null;
        try {
            final KvMetadata patched = client.kv(collection(), key)
                .patch(
                    JsonPatch.builder()
                        .add("lastName", "foo")
                        .move("name", "firstName") // should fail b/c there is no 'name' field
                        .build()
                )
                .get();
            fail("Should have failed with a concurrency conflict.");
        } catch (PatchConflictException ex) {
            thrown = ex;
        }

        assertNotNull(thrown);
        assertEquals(1, (int)thrown.getOpIndex());
        assertEquals("move", thrown.getOp().getOp());

        final KvObject<ObjectNode> kvObject = readItem(key);

        // should not have changed b/c test op failed.
        assertEquals(orig.getRef(), kvObject.getRef());
    }

    @Test
    public void withOpFailureInvalidOp() {
        insertItem(key, "{`name`:`test`}");

        ApiBadRequestException thrown = null;
        try {
            final KvMetadata patched = client.kv(collection(), key)
                .patch(
                    JsonPatch.builder()
                        .op(new JsonPatchOp("bad", "name", "foo"))
                        .build()
                )
                .get();
            fail("Should have failed with a bad request.");
        } catch (ApiBadRequestException ex) {
            thrown = ex;
        }

        assertNotNull(thrown);
    }

    @Test
    public void withIncOp() {
        int value = RAND.nextInt(100000);

        insertItem(key, "{`count`:%d}", value);

        final KvMetadata patched = client.kv(collection(), key)
            .patch(
                JsonPatch.builder()
                    .inc("count")
                    .build()
            )
            .get();

        final KvObject<ObjectNode> kvObject = readItem(key);

        assertEquals(value+1, kvObject.getValue().get("count").intValue());
    }

    @Test
    public void withRemoveOp() {
        int value = RAND.nextInt(100000);

        insertItem(key, "{`count`:%d}", value);

        final KvMetadata patched = client.kv(collection(), key)
            .patch(
                JsonPatch.builder()
                    .remove("count")
                    .build()
            )
            .get();

        final KvObject<ObjectNode> kvObject = readItem(key);

        assertFalse(kvObject.getValue().has("count"));
    }

    @Test
    public void withReplaceOp() {
        int value1 = RAND.nextInt(100000);

        insertItem(key, "{`count`:%d}", value1);

        int value2 = RAND.nextInt(100000);

        final KvMetadata patched = client.kv(collection(), key)
            .patch(
                JsonPatch.builder()
                    .replace("count", value2)
                    .build()
            )
            .get();

        final KvObject<ObjectNode> kvObject = readItem(key);

        assertEquals(value2, kvObject.getValue().get("count").intValue());
    }

    @Test
    public void withReplaceWithNullValue() {
        int value1 = RAND.nextInt(100000);

        insertItem(key, "{`count`:%d}", value1);

        final KvMetadata patched = client.kv(collection(), key)
            .patch(
                JsonPatch.builder()
                    .replace("count", null)
                    .build()
            )
            .get();

        final KvObject<ObjectNode> kvObject = readItem(key);

        assertTrue(kvObject.getValue().has("count"));
        assertTrue(kvObject.getValue().get("count").isNull());
    }

    @Test
    public void withInitOp() {
        insertItem(key, "{`name`:`Test1`}");

        final KvMetadata patched = client.kv(collection(), key)
                .patch(
                        JsonPatch.builder()
                                .init("description", "Test Description")
                                .build()
                )
                .get();

        final KvObject<ObjectNode> kvObject = readItem(key);

        assertEquals("Test Description", kvObject.getValue().get("description").textValue());
    }

    @Test
    public void withInitOpWhenFieldAlreadyPresent() {
        insertItem(key, "{`name`:`Test1`}");

        final KvMetadata patched = client.kv(collection(), key)
                .patch(
                        JsonPatch.builder()
                                .init("name", "Test2")
                                .build()
                )
                .get();

        final KvObject<ObjectNode> kvObject = readItem(key);

        // should still be Test1, since the 'init' only applies if field is not already present
        assertEquals("Test1", kvObject.getValue().get("name").textValue());
    }

    @Test
    public void withMergeOp() {
        insertItem(key, "{`name`:`Test1`}");

        Map<String,Object> merge = new HashMap<String, Object>(1);
        merge.put("description", "Test Description");

        final KvMetadata patched = client.kv(collection(), key)
            .patch(
                JsonPatch.builder()
                    .merge("/", merge)
                    .build()
            )
            .get();

        final KvObject<ObjectNode> kvObject = readItem(key);

        assertEquals("Test1", kvObject.getValue().get("name").textValue());
        assertEquals("Test Description", kvObject.getValue().get("description").textValue());
    }

    @Test
    public void withNestedMergeOp() {
        insertItem(key, "{`name`:`Test1`,`info`:{`phone`:`555-5555`}}");

        Map<String,Object> merge = new HashMap<String, Object>(1);
        merge.put("email", "foo@foo.com");

        final KvMetadata patched = client.kv(collection(), key)
                .patch(
                    JsonPatch.builder()
                        .merge("info", merge)
                        .build()
                )
                .get();

        final KvObject<ObjectNode> kvObject = readItem(key);

        assertEquals("Test1", kvObject.getValue().get("name").textValue());
        assertEquals("555-5555", kvObject.getValue().get("info").get("phone").textValue());
        assertEquals("foo@foo.com", kvObject.getValue().get("info").get("email").textValue());
    }

    @Test
    public void withPatchOp() {
        insertItem(key, "{`name`:`Test1`}");

        final KvMetadata patched = client.kv(collection(), key)
            .patch(
                JsonPatch.builder()
                    .patch("/",
                        JsonPatch.builder()
                            .add("description", "Test Description")
                    )
                    .build()
            )
            .get();

        final KvObject<ObjectNode> kvObject = readItem(key);

        assertEquals("Test1", kvObject.getValue().get("name").textValue());
        assertEquals("Test Description", kvObject.getValue().get("description").textValue());
    }

    @Test
    public void withNestedPatchOp() {
        insertItem(key, "{`name`:`Test1`,`info`:{`phone`:`555-5555`}}");

        final KvMetadata patched = client.kv(collection(), key)
            .patch(
                JsonPatch.builder()
                    .patch("info",
                        JsonPatch.builder()
                            .add("email", "foo@foo.com")
                    )
                    .build()
            )
            .get();

        final KvObject<ObjectNode> kvObject = readItem(key);

        assertEquals("Test1", kvObject.getValue().get("name").textValue());
        assertEquals("555-5555", kvObject.getValue().get("info").get("phone").textValue());
        assertEquals("foo@foo.com", kvObject.getValue().get("info").get("email").textValue());
    }

    @Test
    public void withNestedPatchOpWhenTestOpFails() {
        insertItem(key, "{`a`:0,`b`:0,`c`:0,`d`:0}");

        TestOpApplyException thrown = null;
        try {
            final KvMetadata patched = client.kv(collection(), key)
                .patch(
                    JsonPatch.builder()
                        .inc("a")
                        .patch("/",
                            JsonPatch.builder()
                                .inc("b")
                                .test("a", 100)
                                .inc("c")
                        )
                        .inc("d")
                        .build()
                )
                .get();
        } catch (TestOpApplyException ex) {
            thrown = ex;
        }

        assertNotNull("Should have failed nested test.", thrown);
        final KvObject<ObjectNode> kvObject = readItem(key);

        // all counts should be 0, since none of the patch ops will apply.
        assertEquals(0, kvObject.getValue().get("a").asInt());
        assertEquals(0, kvObject.getValue().get("b").asInt());
        assertEquals(0, kvObject.getValue().get("c").asInt());
        assertEquals(0, kvObject.getValue().get("d").asInt());
    }

    @Test
    public void withConditionalPatchOpCondPasses() {
        insertItem(key, "{`a`:0,`b`:0,`c`:0,`d`:0}");

        TestOpApplyException thrown = null;
        try {
            final KvMetadata patched = client.kv(collection(), key)
                .patch(
                    JsonPatch.builder()
                        .inc("a")
                        .patchIf("/",
                            JsonPatch.builder()
                                .inc("b")
                                .test("a", 1)
                                .inc("c")
                        )
                        .inc("d")
                        .build()
                )
                .get();
        } catch (TestOpApplyException ex) {
            thrown = ex;
        }

        assertNull("Should NOT have failed b/c nested patch is conditional.", thrown);
        final KvObject<ObjectNode> kvObject = readItem(key);

        // all counts should increment b/c the cond inner patch applies
        assertEquals(1, kvObject.getValue().get("a").asInt());
        assertEquals(1, kvObject.getValue().get("b").asInt());
        assertEquals(1, kvObject.getValue().get("c").asInt());
        assertEquals(1, kvObject.getValue().get("d").asInt());
    }

    @Test
    public void withConditionalPatchOpCondFails() {
        insertItem(key, "{`a`:0,`b`:0,`c`:0,`d`:0}");

        TestOpApplyException thrown = null;
        try {
            final KvMetadata patched = client.kv(collection(), key)
                .patch(
                    JsonPatch.builder()
                        .inc("a")
                        .patchIf("/",
                            JsonPatch.builder()
                                .inc("b")
                                .test("a", 100)
                                .inc("c")
                        )
                        .inc("d")
                        .build()
                )
                .get();
        } catch (TestOpApplyException ex) {
            thrown = ex;
        }

        assertNull("Should NOT have failed b/c nested patch is conditional.", thrown);
        final KvObject<ObjectNode> kvObject = readItem(key);

        // inner patch increments (b & c) should be 0, since none of the patch ops will apply.
        assertEquals(0, kvObject.getValue().get("b").asInt());
        assertEquals(0, kvObject.getValue().get("c").asInt());

        // both the outer increments will apply as if the conditional patch was not there.
        assertEquals(1, kvObject.getValue().get("a").asInt());
        assertEquals(1, kvObject.getValue().get("d").asInt());
    }
}
