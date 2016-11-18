package io.orchestrate.client;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * This class represents the results of a StatsAggregate function, providing
 * the minimum, maximum, mean, sum, sum-of-squares, variance, and standard
 * deviation for all numeric field values of the designated database field.
 */
public class StatsAggregateResult extends AggregateResult {

    private final double min;
    private final double max;
    private final double mean;
    private final double sum;
    private final double sumOfSquares;
    private final double variance;
    private final double stdDev;

    StatsAggregateResult(
        String fieldName,
        long valueCount,
        double min,
        double max,
        double mean,
        double sum,
        double sumOfSquares,
        double variance,
        double stdDev
    ) {
        super(fieldName, "stats", valueCount);
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.sum = sum;
        this.sumOfSquares = sumOfSquares;
        this.variance = variance;
        this.stdDev = stdDev;
    }

    /**
     * Returns the minimum value of all numerical values included in this aggregate
     * @return the minimum value
     */
    public double getMin() {
        return min;
    }

    /**
     * Returns the maximum value of all numerical values included in this aggregate
     * @return the maximum value
     */
    public double getMax() {
        return max;
    }

    /**
     * Returns the average value of all numerical values included in this aggregate
     * @return the average value
     */
    public double getMean() {
        return mean;
    }

    /**
     * Returns the sum of all numerical values included in this aggregate
     * @return the sum
     */
    public double getSum() {
        return sum;
    }

    /**
     * Returns the sum of squares for all numerical values included in this aggregate
     * @return the sum of squares
     */
    public double getSumOfSquares() {
        return sumOfSquares;
    }

    /**
     * Returns the variance of all numerical values included in this aggregate
     * @return the variance
     */
    public double getVariance() {
        return variance;
    }

    /**
     * Returns the standard deviation of all numerical values included in this aggregate
     * @return the standard deviation
     */
    public double getStdDev() {
        return stdDev;
    }

    static StatsAggregateResult from(JsonNode json) {

        String fieldName = json.get("field_name").asText();
        long valueCount = json.get("value_count").asLong();

        String aggregateKind = json.get("aggregate_kind").asText();
        assert aggregateKind.equals("stats");

        JsonNode statistics = json.get("statistics");
        double min = statistics.get("min").asDouble();
        double max = statistics.get("max").asDouble();
        double mean = statistics.get("mean").asDouble();
        double sum = statistics.get("sum").asDouble();
        double sumOfSquares = statistics.get("sum_of_squares").asDouble();
        double variance = statistics.get("variance").asDouble();
        double stdDev = statistics.get("std_dev").asDouble();

        return new StatsAggregateResult(
            fieldName, valueCount,
            min, max, mean, sum,
            sumOfSquares, variance, stdDev
        );
    }

}
