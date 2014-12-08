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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class JsonPatchOp {
    private final String op;
    private final String path;
    private final Object value;
    private final String from;

    public JsonPatchOp(String op, String path, Object value) {
        this(op, path, value, null);
    }

    @JsonCreator
    public JsonPatchOp(@JsonProperty("op") String op, @JsonProperty("path") String path, @JsonProperty("value") Object value, @JsonProperty("from") String from) {
        this.op = op;
        this.path = path;
        this.value = value;
        this.from = from;
    }

    public String getOp() {
        return op;
    }

    public String getPath() {
        return path;
    }

    public Object getValue() {
        return value;
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    public String getFrom() {
        return from;
    }
}
