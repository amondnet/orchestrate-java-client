package io.orchestrate.client;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * This class represents the results of a TimeSeriesAggregate function. The list
 * of TimeSeriesBucket objects included with this aggregate indicate how many
 * date values in the designated database field were found within each interval.
 */
public class TimeSeriesAggregateResult extends AggregateResult {

    private final TimeInterval interval;

    private final List<TimeSeriesBucket> buckets;

    TimeSeriesAggregateResult(
        String fieldName,
        long valueCount,
        TimeInterval interval,
        List<TimeSeriesBucket> buckets
    ) {
        super(fieldName, "time_series", valueCount);
        this.interval = interval;
        this.buckets = buckets;
    }

    /**
     * Returns bucketing interval for this TimeSeries.
     *
     * @return The TimeInterval object.
     */
    public TimeInterval getInterval() {
        return interval;
    }

    /**
     * Returns a list of TimeSeriesBuckets representing the number of field values
     * found within each time interval.
     *
     * @return The time series buckets.
     */    
    public List<TimeSeriesBucket> getBuckets() {
        return buckets;
    }

    static TimeSeriesAggregateResult from(JsonNode json) {

        String fieldName = json.get("field_name").asText();
        long valueCount = json.get("value_count").asLong();

        String aggregateKind = json.get("aggregate_kind").asText();
        assert aggregateKind.equals("time_series");

        TimeInterval interval = TimeInterval.valueOf(json.get("interval").asText().toUpperCase());
        ArrayNode bucketNodes = (ArrayNode) json.get("buckets");
        List<TimeSeriesBucket> buckets = new ArrayList<TimeSeriesBucket>(bucketNodes.size());
        for (JsonNode bucketNode : bucketNodes) {
            String bucket = bucketNode.get("bucket").asText();
            long count = bucketNode.get("count").asLong();
            buckets.add(new TimeSeriesBucket(bucket, count));
        }

        return new TimeSeriesAggregateResult(fieldName, valueCount, interval, buckets);
    }

}
