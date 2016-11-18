package io.orchestrate.client;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * This class represents the results of a RangeAggregate function. The list of
 * RangeBucket objects included with this aggregate indicate how many numeric
 * values in the designated database field were found within each interval.
 */
public class RangeAggregateResult extends AggregateResult {
    
    private final List<RangeBucket> buckets;

    RangeAggregateResult(
        String fieldName,
        long valueCount,
        List<RangeBucket> buckets
    ) {
        super(fieldName, "range", valueCount);
        this.buckets = buckets;
    }

    /**
     * Returns a list of RangeBuckets representing the number of numeric field values
     * found within each interval.
     *
     * @return The range buckets.
     */
    public List<RangeBucket> getBuckets() {
        return buckets;
    }

    static RangeAggregateResult from(JsonNode json) {

        String fieldName = json.get("field_name").asText();
        long valueCount = json.get("value_count").asLong();

        String aggregateKind = json.get("aggregate_kind").asText();
        assert aggregateKind.equals("range");

        ArrayNode bucketNodes = (ArrayNode) json.get("buckets");
        List<RangeBucket> buckets = new ArrayList<RangeBucket>(bucketNodes.size());
        for (JsonNode bucketNode : bucketNodes) {
            double min = Double.NEGATIVE_INFINITY;
            if (bucketNode.has("min")) {
                min = bucketNode.get("min").asDouble();
            }
            double max = Double.POSITIVE_INFINITY;
            if (bucketNode.has("max")) {
                max = bucketNode.get("max").asDouble();
            }
            long count = bucketNode.get("count").asLong();
            buckets.add(new RangeBucket(min, max, count));
        }

        return new RangeAggregateResult(fieldName, valueCount, buckets);
    }

}
