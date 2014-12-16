package io.orchestrate.client;

/**
 * This class represents numeric ranges, with minimum and maximum points,
 * that can be used to build Range or Distance aggregates. Although all
 * Range objects require a min and max value, an unbounded range can be created using
 * Double.POSITIVE_INFINITY or Double.NEGATIVE_INFINITY.
 *
 * For the sake of building aggregate queries, Ranges are always inclusive
 * of the lower bound and exclusive of the upper bound. So, for example, consider
 * two consecutive Range objects with adjacent bounds:
 *
 * <pre>
 * {@code
 *   Range a = Range.between(0, 10);
 *   Range b = Range.between(10, 20);
 * }
 * </pre>
 *
 * The value 10 would be included as part of range `b` but not as part of
 * range `a`, since ranges are always exclusive of their max value.
 *
 * Consumers of this class should use the public static factory methods
 * (below, above, and between) to create Ranges, since those methods handle
 * infinities and NaN values correctly.
 */
public class Range {

    private final double min;

    private final double max;

    Range(double min, double max) {
        if ((Double.isNaN(min))) {
            throw new IllegalArgumentException("Can't create a Range with NaN min value");
        }
        if ((Double.isNaN(max))) {
            throw new IllegalArgumentException("Can't create a Range with NaN max value");
        }
        if (min == max) {
            throw new IllegalArgumentException(String.format(
                "Can't create a Range with identical min and max (%s)", min
            ));
        }
        if (min > max) {
            throw new IllegalArgumentException(String.format(
                "Can't create a Range where min (%s) is greater than max (%s)", min, max
            ));
        }
        this.min = min;
        this.max = max;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    /**
     * Creates a new Range object with the designated upper bound and Double.NEGATIVE_INFINITY
     * as the lower bound.
     *
     * @param max The upper bound for the range.
     * @return A range object.
     */
    public static Range below(double max) {
        return new Range(Double.NEGATIVE_INFINITY, max);
    }

    /**
     * Creates a new Range object with the designated lower bound and Double.POSITIVE_INFINITY
     * as the upper bound.
     *
     * @param min The lower bound for the range.
     * @return A range object.
     */
    public static Range above(double min) {
        return new Range(min, Double.POSITIVE_INFINITY);
    }

    /**
     * Creates a new Range object with the designated lower and upper bounds.
     *
     * @param min The lower bound for the range.
     * @param max The upper bound for the range.
     * @return A range object.
     */
    public static Range between(double min, double max) {
        return new Range(min, max);
    }

    /**
     * Creates a syntactically correct string version of this range, suitable
     * for building range aggregate and distance aggregate clauses.
     *
     * @return The string representation of the range.
     */
    public String unparse() {
        StringBuilder b = new StringBuilder();
        if (min == Double.NEGATIVE_INFINITY) {
            b.append('*');
        } else {
            b.append(Double.toString(min));
        }
        b.append('~');
        if (max == Double.POSITIVE_INFINITY) {
            b.append('*');
        } else {
            b.append(Double.toString(max));
        }
        return b.toString();
    }

}
