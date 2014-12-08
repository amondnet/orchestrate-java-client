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
package io.orchestrate.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.orchestrate.client.jsonpatch.JsonPatchOp;

public class PatchConflictException extends RequestException {
    private Integer opIndex;
    private JsonPatchOp op;

    PatchConflictException(int statusCode, JsonNode json, String rawResponse, String requestId) {
        super(statusCode, json, rawResponse, requestId);
        if(json != null && json.has("details")) {
            JsonNode details = json.get("details");
            if(details.has("opIndex")) {
                opIndex = details.get("opIndex").asInt();
            }
            if(details.has("op")) {
                try {
                    this.op = MAPPER.treeToValue(details.get("op"), JsonPatchOp.class);
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
        }
    }

    /**
     * The index of the operation that failed if provided in the response.
     * @return The index of the operation that failed, null if not present.
     */
    public Integer getOpIndex() {
        return opIndex;
    }

    /**
     * The operation that failed, if provided in the response.
     * @return The failed operation, null if not present.
     */
    public JsonPatchOp getOp() {
        return op;
    }
}
