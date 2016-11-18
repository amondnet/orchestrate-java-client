package io.orchestrate.client;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ItemKind {
    ITEM,
    EVENT,
    RELATIONSHIP;

    @JsonCreator
    public static ItemKind fromJson(String name) {
        return ItemKind.valueOf(name.toUpperCase());
    }
}
