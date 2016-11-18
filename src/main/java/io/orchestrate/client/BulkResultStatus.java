package io.orchestrate.client;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BulkResultStatus {
    SUCCESS,
    FAILURE;

    @JsonCreator
    public static BulkResultStatus fromJson(String status) {
        return BulkResultStatus.valueOf(status.toUpperCase());
    }
}
