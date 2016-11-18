package io.orchestrate.client;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * This class represents the results of a TopValuesAggregate function, providing
 * the minimum, maximum, mean, sum, sum-of-squares, variance, and standard
 * deviation for all numeric field values of the designated database field.
 */
public class TopValuesAggregateResult extends AggregateResult {

    private final List<CountedValue> entries;

    private final int offset;
    private final int limit;

    TopValuesAggregateResult(
        String fieldName,
        long valueCount,
        List<CountedValue> entries,
        int offset,
        int limit
    ) {
        super(fieldName, "top_values", valueCount);
        this.entries = entries;
        this.offset = offset;
        this.limit = limit;
    }

    /**
     * Returns the paged list of entries for this top-values aggregate, with paging
     * corresponding to the offset and limit parameters of the original request.
     * @return
     */
    public List<CountedValue> getEntries() {
        return entries;
    }

    /**
     * Returns the offset parameter for this top-values aggregate, as designated in the
     * original request. This param represents the index of the first top-values result
     * entry to return in this paged result set. If the full aggregate result set contains
     * fewer unique values than requested, this paged aggregate result set will be empty.
     * @return the offset parameter
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Returns the limit parameter for this top-values aggregate, as designated in the
     * original request. This param represents the maximum number of entries to return
     * in this paged result set. However, if the full aggregate result set contains
     * fewer unique values than the sum of the offset plus limit, then the paged
     * aggregate result may have fewer items than requested.
     * @return the limit parameter
     */
    public double getLimit() {
        return limit;
    }

    static TopValuesAggregateResult from(JsonNode json) {

        String fieldName = json.get("field_name").asText();
        long valueCount = json.get("value_count").asLong();

        String aggregateKind = json.get("aggregate_kind").asText();
        assert aggregateKind.equals("top_values");

        int offset = json.get("offset").asInt();
        int limit = json.get("limit").asInt();

        List<CountedValue> entries = new ArrayList<CountedValue>();
        for (JsonNode entryNode : (ArrayNode) json.get("entries")) {
            JsonNode valueNode = entryNode.get("value");
            long count = entryNode.get("count").asLong();
            if (valueNode == null || valueNode.isNull()) {
                entries.add(new CountedValue(null, count));
            } else if (valueNode.isBoolean()) {
                entries.add(new CountedValue(valueNode.asBoolean(), count));
            } else if (valueNode.isNumber()) {
                entries.add(new CountedValue(valueNode.asDouble(), count));
            } else if (valueNode.isTextual()) {
                entries.add(new CountedValue(valueNode.asText(), count));
            }
        }

        return new TopValuesAggregateResult(fieldName, valueCount, entries, offset, limit);
    }

}
