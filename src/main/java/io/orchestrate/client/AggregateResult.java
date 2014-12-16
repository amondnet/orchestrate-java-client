package io.orchestrate.client;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * This abstract class represents the result of an arbitrary aggregate function. All
 * AggregateResult objects operate over a particular field name, with a particular kind
 * of aggregate function, and provide a count of the number of field values included
 * in the aggregate calculation.
 *
 * Individual subclass implementations will augment this basic information with specific
 * details relevant to the kind of aggregation performed.
 */
public abstract class AggregateResult {

    private final String fieldName;
    private final String aggregateKind;
    private final long valueCount;

    AggregateResult(String fieldName, String aggregateKind, long valueCount) {
        this.fieldName = fieldName;
        this.aggregateKind = aggregateKind;
        this.valueCount = valueCount;
    }

    static List<AggregateResult> from(ArrayNode aggregateNodes) {
        List<AggregateResult> aggregates = new ArrayList<AggregateResult>(aggregateNodes.size());
        for (JsonNode aggregateNode : aggregateNodes) {
            String aggregateKind = aggregateNode.get("aggregate_kind").asText();
            AggregateResult aggregate = null;
            if (aggregateKind.equals("stats")) {
                aggregate = StatsAggregateResult.from(aggregateNode);
            } else if (aggregateKind.equals("range")) {
                aggregate = RangeAggregateResult.from(aggregateNode);
            } else if (aggregateKind.equals("distance")) {
                aggregate = DistanceAggregateResult.from(aggregateNode);
            } else if (aggregateKind.equals("time_series")) {
                aggregate = TimeSeriesAggregateResult.from(aggregateNode);
            } else {
                throw new RuntimeException("Unexpected aggregate kind: " + aggregateKind);
            }
            aggregates.add(aggregate);
        }
        return aggregates;
    }

    /**
     * Returns the fully-qualified name of the field used by this aggregate.
     *
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the kind of aggregate represented in this result. Possible values
     * include: stats, range, distance, and time_series
     *
     * @return the aggregate kind
     */
    public String getAggregateKind() {
        return aggregateKind;
    }

    /**
     * Returns the total number of database field values included in this aggregate.
     * It's important to note that this is a count of the field values, rather than
     * a count of database records, since each record can contain multiple values
     * for each field.
     *
     * @return the total value count
     */
    public long getValueCount() {
        return valueCount;
    }

}
