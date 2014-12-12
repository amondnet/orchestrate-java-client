/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.orchestrate.client;

/**
 * A helper class to handle precondition checking in method calls.
 */
public final class Preconditions {

    /**
     * Checks that the {@code value} is not {@code null}. Returns the value
     * directly, so you can use checkNotNull(value) inline.
     *
     * @param value The object to perform the {@code null} check.
     * @param paramName The name of the parameter that cannot be {@code null}.
     * @param <T> The type of the value being checked.
     * @return The value that was checked.
     */
    public static <T> T checkNotNull(final T value, final String paramName) {
        if (value == null) {
            final String msg = String.format("'%s' cannot be null.", paramName);
            throw new NullPointerException(msg);
        }
        return value;
    }

    /**
     * Checks that the {@code value} is not empty.
     *
     * @param value The object to perform the check on.
     * @param paramName The name of the parameter that can't be empty.
     * @return The value that was checked.
     */
    public static String checkNotNullOrEmpty(final String value, final String paramName) {
        checkNotNull(value, paramName);
        if (value.length() < 1) {
            final String msg = String.format("'%s' cannot be empty.", paramName);
            throw new IllegalArgumentException(msg);
        }
        return value;
    }

    /**
     * Checks that the condition is {@code true}. Use for validating arguments
     * to methods.
     *
     * @param condition The condition to check.
     * @param errorMsg The error message to display if the check was {@code false}.
     */
    public static void checkArgument(final boolean condition, final String errorMsg) {
        if (!condition) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Checks that the {@code value} is not negative.
     *
     * @param value The object to perform the check on.
     * @param paramName The name of the parameter that can't be negative.
     * @return The value that was checked.
     */
    public static int checkNotNegative(final int value, final String paramName) {
        if (value < 0) {
            final String msg = String.format("'%s' cannot be negative.", paramName);
            throw new IllegalArgumentException(msg);
        }
        return value;
    }

    /**
     * Checks that the {@code value} is not negative.
     *
     * @param value The object to perform the check on.
     * @param paramName The name of the parameter that can't be negative.
     * @return The value that was checked.
     */
    public static long checkNotNegative(final long value, final String paramName) {
        if (value < 0) {
            final String msg = String.format("'%s' cannot be negative.", paramName);
            throw new IllegalArgumentException(msg);
        }
        return value;
    }

    /**
     * Checks that the {@code values} are not empty.
     *
     * @param values The values to check.
     * @param paramName The name of the parameter that is to be checked.
     * @param itemName The name of an item from the values.
     * @return The value that was checked.
     */
    public static String[] checkNoneEmpty(final String[] values, final String paramName, final String itemName) {
        if (values == null || values.length < 1) {
            final String msg = String.format("'%s' cannot be empty.", paramName);
            throw new IllegalArgumentException(msg);
        }
        for (String item : values) {
            checkNotNullOrEmpty(item, itemName);
        }
        return values;
    }

}
