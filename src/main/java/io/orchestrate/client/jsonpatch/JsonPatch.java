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
            ops.add(new JsonPatchOp("add", path, value));
            return this;
        }

        public Builder test(String path, Object value) {
            ops.add(new JsonPatchOp("test", path, value));
            return this;
        }

        public Builder move(String from, String path) {
            ops.add(new JsonPatchOp("move", path, null, from));
            return this;
        }

        public Builder copy(String from, String path) {
            ops.add(new JsonPatchOp("copy", path, null, from));
            return this;
        }

        public Builder inc(String path) {
            inc(path, null);
            return this;
        }

        public Builder inc(String path, Number value) {
            ops.add(new JsonPatchOp("inc", path, value));
            return this;
        }

        public Builder remove(String path) {
            ops.add(new JsonPatchOp("remove", path, null, null));
            return this;
        }

        public Builder replace(String path, Object value) {
            ops.add(new JsonPatchOp("replace", path, value, null));
            return this;
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
