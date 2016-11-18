package io.orchestrate.client;

import com.fasterxml.jackson.annotation.*;
import lombok.NonNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventPath extends ItemPath {
    @JsonProperty
    private String type;
    @JsonProperty
    private Long timestamp;
    @JsonProperty
    private String ordinal;
    @JsonProperty
    private String ordinal_str;

    protected EventPath(
            @NonNull String collection,
            @NonNull String key,
            @NonNull String type,
            @NonNull Long timestamp) {
        super(collection, key, ItemKind.EVENT);

        this.type = type;
        this.timestamp = timestamp;
    }

    // Used for JSON serialization
    protected EventPath() {}

    public String getType() {
        return type;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getOrdinal() {
        return ordinal;
    }
}
