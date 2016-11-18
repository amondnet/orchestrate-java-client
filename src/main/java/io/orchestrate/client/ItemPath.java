package io.orchestrate.client;

import com.fasterxml.jackson.annotation.*;
import lombok.NonNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemPath {
    @JsonProperty
    private String collection;
    @JsonProperty
    private String key;
    @JsonProperty
    private ItemKind kind;
    @JsonProperty
    private String ref;
    @JsonProperty
    private Long reftime;

    public ItemPath(@NonNull String collection, @NonNull String key) {
        this(collection, key, null);
    }

    public ItemPath(@NonNull String collection, @NonNull String key, ItemKind kind) {
        this.collection = collection;
        this.key = key;
        this.kind = kind;
    }

    // Used for JSON serialization
    protected ItemPath() {}

    public String getCollection() { return collection; }
    public String getKey() { return key; }
    public ItemKind getKind() { return kind; }
    public String getRef() { return ref; }
    public Long getReftime() { return reftime; }
}
