package io.orchestrate.client;

/**
 * This class represents a numeric range, with an associated count value,
 * used to represent the results of a RangeAggregate or DistanceAggregate.
 */
public class RangeBucket extends Range {

    private final long count;

    RangeBucket(double min, double max, long count) {
        super(min, max);
        if (count < 0) {
            throw new IllegalArgumentException(String.format(
                "Can't create a RangeBucket with negative count (%s)", count
            ));
        }
        this.count = count;
    }

    /**
     * Returns the number of field values falling within the bounds of
     * this numeric range, in the context of a RangeAggregateResult or
     * DistanceAggregateResult.
     *
     * @return The number of values within the numeric range.
     */
    public long getCount() {
        return count;
    }

}
