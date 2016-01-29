package io.orchestrate.client;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BulkStatus {
    SUCCESS,
    FAILURE,
    PARTIAL;

    @JsonCreator
    public static BulkStatus fromJson(String name) {
        return BulkStatus.valueOf(name.toUpperCase());
    }
}
