package io.orchestrate.client;

import static io.orchestrate.client.Preconditions.checkNotNull;

/**
 * This is a convenience class that helps build syntactically correct
 * aggregate function clauses that can be added to a search query.
 */
public class Aggregate {

    private final StringBuilder b;

    /*
     * Private constructor. Consumers should use the public static builder method.
     */
    private Aggregate() {
        b = new StringBuilder();
    }

    /**
     * Creates a new empty Aggregate object, which can be used to build a collection
     * of aggregate clauses and form them into a syntactically correct string.
     *
     * @return An instance of the builder.
     */
    public static Aggregate builder() {
        return new Aggregate();
    }

    /**
     * Builds the final stringified version of your aggregate clause.
     *
     * @return The string representing the Aggregates query.
     */
    public String build() {
        return b.toString();
    }

    /**
     * Adds a statistical aggregate to the query for the given field name
     *
     * <p>
     * {@code
     * client.searchCollection("someCollection")
     *     .aggregate(Aggregate.builder()
     *         .stats("value.cart.items.price")
     *         .build()
     *     )
     *     .get(String.class, "*")
     *     .get()
     * }
     * </p>
     *
     * @param fieldName The fully-qualified name of the field to aggregate upon
     * @return This request.
     */
    public Aggregate stats(final String fieldName) {
        checkNotNull(fieldName, "fieldName");
        if (b.length() > 0) {
            b.append(',');
        }
        b.append(String.format("%s:stats", fieldName));
        return this;
    }

    /**
     * Adds a range aggregate to the query for the given field name
     *
     * <p>
     * {@code
     * client.searchCollection("someCollection")
     *     .aggregate(Aggregate.builder()
     *         .range(
     *             "value.inventory.quantity",
     *             Range.below(10),
     *             Range.between(10, 100),
     *             Range.above(100)
     *         )
     *         .build()
     *     )
     *     .get(String.class, "*")
     *     .get()
     * }
     * </p>
     *
     * @param fieldName The fully-qualified name of the field to aggregate upon
     * @param range The first numeric range to use as a histogram bucket
     * @param ranges A varargs list of zero or more additional numeric ranges to use as histogram buckets
     * @return This request.
     */
    public Aggregate range(final String fieldName, Range range, Range... ranges) {
        checkNotNull(fieldName, "fieldName");
        checkNotNull(range, "range");
        if (b.length() > 0) {
            b.append(',');
        }
        b.append(fieldName);
        b.append(":range:");
        b.append(range.unparse());
        for (Range r : ranges) {
            checkNotNull(r, "ranges");
            b.append(':');
            b.append(r.unparse());
        }
        return this;
    }

    /**
     * Adds a distance aggregate to the query for the given field name.
     *
     * It's important to note that distance aggregates can only be used when the lucene
     * query contains a NEAR clause.
     *
     * <p>
     * {@code
     * client.searchCollection("someCollection")
     *     .aggregate(Aggregate.builder()
     *         .distance(
     *             "value.location.geo",
     *             Range.below(10),
     *             Range.between(10, 100),
     *             Range.above(100)
     *         )
     *         .build()
     *     )
     *     .get(String.class, "value.location:NEAR:{ lat: 12.34 lon: 56.78 dist: 1000km }")
     *     .get()
     * }
     * </p>
     *
     * @param fieldName The fully-qualified name of the field to aggregate upon
     * @param range The first numeric range to use as a histogram bucket
     * @param ranges A varargs list of zero or more additional numeric ranges to use as histogram buckets
     * @return This request.
     */
    public Aggregate distance(final String fieldName, Range range, Range... ranges) {
        checkNotNull(fieldName, "fieldName");
        checkNotNull(range, "range");
        if (b.length() > 0) {
            b.append(',');
        }
        b.append(fieldName);
        b.append(":distance:");
        b.append(range.unparse());
        for (Range r : ranges) {
            checkNotNull(r, "ranges");
            b.append(':');
            b.append(r.unparse());
        }
        return this;
    }

    /**
     * Adds a time-series aggregate to the query for the given field name,
     * with bucket intervals based on the UTC time zone.
     *
     * <p>
     * {@code
     * client.searchCollection("someCollection")
     *     .aggregate(Aggregate.builder()
     *         .timeSeries("value.date_of_birth", TimeInterval.MONTH)
     *         .build()
     *     )
     *     .get(String.class, "*")
     *     .get()
     * }
     * </p>
     *
     * @param fieldName The fully-qualified name of the field to aggregate upon
     * @param interval The time interval to bucket upon
     * @return This request.
     */
    public Aggregate timeSeries(final String fieldName, TimeInterval interval) {
        return timeSeries(fieldName, interval, null);
    }

    /**
     * Adds a time-series aggregate to the query for the given field name,
     * with bucket intervals based on the designated time zone.
     * 
     * Time zone strings must begin with a "+" or "-" character, followed by
     * four digits representing the hours and minutes of offset, relative to
     * UTC. For example, Eastern Standard Time (EST) would be represented as
     * "-0500", since the time in EST is five hours behind that of UTC. 
     *
     * <p>
     * {@code
     * client.searchCollection("someCollection")
     *     .aggregate(Aggregate.builder()
     *         .timeSeries("value.date_of_birth", TimeInterval.MONTH, "-0500")
     *         .build()
     *     )
     *     .get(String.class, "*")
     *     .get()
     * }
     * </p>
     *
     * @param fieldName The fully-qualified name of the field to aggregate upon
     * @param interval The time interval to bucket upon
     * @param timeZone The time zone to use when computing interval bucket boundaries
     * @return This request.
     */
    public Aggregate timeSeries(final String fieldName, TimeInterval interval, String timeZone) {
        checkNotNull(fieldName, "fieldName");
        checkNotNull(interval, "interval");
        if (b.length() > 0) {
            b.append(',');
        }
        b.append(fieldName);
        b.append(":time_series:");
        b.append(interval.toString().toLowerCase());
        if (timeZone != null) {
            b.append(':');
            b.append(timeZone);
        }
        return this;
    }
}
