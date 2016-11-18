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
package io.orchestrate.client.jsonpatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple wrapper and builder for creating Patch Operations used for partial updates.
 *
 * https://tools.ietf.org/html/rfc6902
 */
public class JsonPatch {
    private final List<JsonPatchOp> ops;

    public JsonPatch(List<JsonPatchOp> ops) {
        this.ops = ops;
    }

    public List<JsonPatchOp> getOps() {
        return ops;
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder {
        List<JsonPatchOp> ops = new ArrayList<JsonPatchOp>();
        public Builder add(String path, Object value) {
            return op(new JsonPatchOp("add", path, value));
        }

        public Builder test(String path, Object value) {
            return op(TestOp.matches(path, value));
        }

        public Builder testNot(String path, Object value) {
            return op(TestOp.notMatches(path, value));
        }

        public Builder testFieldPresent(String path) {
            return op(TestOp.fieldPresent(path));
        }

        public Builder testFieldMissing(String path) {
            return op(TestOp.fieldMissing(path));
        }

        public Builder move(String from, String path) {
            return op(new JsonPatchOp("move", path, null, from));
        }

        public Builder copy(String from, String path) {
            return op(new JsonPatchOp("copy", path, null, from));
        }

        public Builder inc(String path) {
            return inc(path, null);
        }

        public Builder inc(String path, Number value) {
            return op(new JsonPatchOp("inc", path, value));
        }

        public Builder remove(String path) {
            return op(new JsonPatchOp("remove", path, null, null));
        }

        public Builder replace(String path, Object value) {
            return op(new JsonPatchOp("replace", path, value, null));
        }

        public Builder init(String path, Object value) {
            return op(new JsonPatchOp("init", path, value, null));
        }

        public Builder merge(String path, Object value) {
            return op(new JsonPatchOp("merge", path, value, null));
        }

        public Builder patch(String path, JsonPatch patch) {
            return patch(path, patch, false);
        }

        public Builder patch(String path, Builder patch) {
            return patch(path, patch, false);
        }

        public Builder patchIf(String path, Builder patch) {
            return patch(path, patch, true);
        }

        public Builder patch(String path, JsonPatch patch, boolean conditional) {
            return op(new InnerPatchPatchOp(path, patch, conditional));
        }

        public Builder patch(String path, Builder patch, boolean conditional) {
            return patch(path, patch.build(), conditional);
        }

        /**
         * This allows for better forward compatibility. If orchestrate introduces a new Patch Op,
         * older versions of the Java client will be able to use it via this method.
         * @param op The patch operation to add.
         * @return this builder.
         */
        public Builder op(JsonPatchOp op) {
            ops.add(op);
            return this;
        }

        public JsonPatch build() {
            return new JsonPatch(ops);
        }
    }
}
