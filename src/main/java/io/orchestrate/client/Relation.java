package io.orchestrate.client;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Getter;

@AllArgsConstructor
public class Relation implements RelationMetadata {

    @NonNull @Getter
    private final String sourceCollection;

    @NonNull @Getter
    private final String sourceKey;

    @NonNull @Getter
    private final String destinationCollection;

    @NonNull @Getter
    private final String destinationKey;

    @NonNull @Getter
    private final String kind;

    @NonNull @Getter
    private final String ref;

    @Getter
    private final JsonNode value;

}
