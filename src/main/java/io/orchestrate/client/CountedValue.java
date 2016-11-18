package io.orchestrate.client;

/**
 * This class represents a value in a top-values aggregate, coupled with a count
 * of how many times that value occurred within the context of the aggregate.
 */
public class CountedValue {

    private final Object value;
    private final long count;

    CountedValue(boolean value, long count) {
        this.value = value;
        this.count = count;
    }

    CountedValue(double value, long count) {
        this.value = value;
        this.count = count;
    }

    CountedValue(String value, long count) {
        this.value = value;
        this.count = count;
    }

    /**
     * Returns the value of this item.
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Returns the number of times this value occurred within the context of
     * the current top-values aggregate.
     * @return
     */
    public long getCount() {
        return count;
    }

}
