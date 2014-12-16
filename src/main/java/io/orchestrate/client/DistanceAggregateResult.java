package io.orchestrate.client;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * This class represents the results of a DistanceAggregate function. The list of
 * RangeBucket objects included with this aggregate indicate how many geo-point
 * objects in the designated database field were found within each distance
 * interval from the central anchor point.
 */
public class DistanceAggregateResult extends AggregateResult {

    private final List<RangeBucket> buckets;

    DistanceAggregateResult(
        String fieldName,
        long valueCount,
        List<RangeBucket> buckets
    ) {
        super(fieldName, "distance", valueCount);
        this.buckets = buckets;
    }

    /**
     * Returns a list of RangeBuckets representing the number of geo-point field values
     * found within each distance interval from the central anchor point designated in
     * the accompanying distance query (inside the mandatory NEAR clause).
     *
     * Likewise, the bounds of the range are expressed in terms of the same distance units
     * (miles, kilometers, etc) used within the accompanying distance query.
     *
     * @return The range buckets.
     */
    public List<RangeBucket> getBuckets() {
        return buckets;
    }

    static DistanceAggregateResult from(JsonNode json) {

        String fieldName = json.get("field_name").asText();
        long valueCount = json.get("value_count").asLong();

        String aggregateKind = json.get("aggregate_kind").asText();
        assert aggregateKind.equals("distance");

        ArrayNode bucketNodes = (ArrayNode) json.get("buckets");
        List<RangeBucket> buckets = new ArrayList<RangeBucket>(bucketNodes.size());
        for (JsonNode bucketNode : bucketNodes) {
            double min = 0;
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

        return new DistanceAggregateResult(fieldName, valueCount, buckets);
    }

}
