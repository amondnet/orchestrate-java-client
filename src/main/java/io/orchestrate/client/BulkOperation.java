package io.orchestrate.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Describes a bulk operation.
 * @see Client#bulk()
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkOperation {
    @JsonProperty
    ItemPath path;
    @JsonProperty
    private ItemPath source;
    @JsonProperty
    private ItemPath destination;
    @JsonProperty
    private ItemKind kind;
    @JsonProperty
    private String relation;
    @JsonProperty
    @JsonSerialize(using = StringToRawJsonSerializer.class)
    private Object value;

    protected static BulkOperation forKvItem(String collection, String key, Object value) {
        BulkOperation bulkOperation = new BulkOperation();
        bulkOperation.path = new ItemPath(collection, key, ItemKind.ITEM);
        bulkOperation.value = value;
        return bulkOperation;
    }

    protected static BulkOperation forEvent(String collection, String key, String type, Long timestamp, Object value) {
        BulkOperation bulkOperation = new BulkOperation();
        bulkOperation.path = new EventPath(collection, key, type, timestamp);
        bulkOperation.value = value;
        return bulkOperation;
    }

    protected static BulkOperation forRelationship(String sourceCollection,
                                         String sourceKey,
                                         String destCollection,
                                         String destKey,
                                         String relation,
                                         Object properties) {
        BulkOperation bulkOperation = new BulkOperation();
        bulkOperation.source = new ItemPath(sourceCollection, sourceKey);
        bulkOperation.destination = new ItemPath(destCollection, destKey);
        bulkOperation.relation = relation;
        bulkOperation.value = properties;
        return bulkOperation;
    }
}
