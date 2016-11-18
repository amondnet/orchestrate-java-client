package io.orchestrate.client.jsonpatch;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

@JsonSerialize(using = TestOp.TestOpSerializer.class)
public class TestOp extends JsonPatchOp {
    public static final Object ANY_VALUE = new Object();

    private final boolean negate;

    public TestOp(String path, Object value) {
        this(path, value, false);
    }

    public TestOp(String path, Object value, boolean negate) {
        super("test", path, value);
        this.negate = negate;
    }

    public boolean isNegate() {
        return negate;
    }

    public TestOp negate() {
        return new TestOp(getPath(), getValue(), true);
    }

    public static TestOp fieldPresent(String path) {
        return new TestOp(path, ANY_VALUE);
    }

    public static TestOp fieldMissing(String path) {
        return new TestOp(path, ANY_VALUE, true);
    }

    public static TestOp matches(String path, Object value) {
        return new TestOp(path, value);
    }

    public static TestOp notMatches(String path, Object value) {
        return new TestOp(path, value, true);
    }

    static class TestOpSerializer extends JsonSerializer<TestOp> {
        @Override
        public void serialize(TestOp op, JsonGenerator jsonGenerator,
                              SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("op", "test");
            jsonGenerator.writeStringField("path", op.getPath());
            if (op.getValue() != ANY_VALUE) {
                jsonGenerator.writeFieldName("value");
                jsonGenerator.getCodec().writeValue(jsonGenerator, op.getValue());
            }
            if (op.isNegate()) {
                jsonGenerator.writeBooleanField("negate", true);
            }
            jsonGenerator.writeEndObject();
        }
    }
}
