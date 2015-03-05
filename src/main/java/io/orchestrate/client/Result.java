/*
 * Copyright 2014 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.annotation.Nullable;

/**
 * A container for the search result and its associated KV data.
 *
 * @param <T> The deserializable type for the value of the KV data belonging
 *            to this search result.
 */
@ToString
@EqualsAndHashCode
public class Result<T> {

    /** The KV object for this search result. */
    private final KvObject<T> kvObject;
    /** The score for this result. */
    private final double score;
    /** The distance of a result, present in geospatial queries. */
    private final Double distance;

    Result(final KvObject<T> kvObject, final double score, final Double distance) {
        assert (kvObject != null);
        assert (score >= 0);

        this.kvObject = kvObject;
        this.score = score;
        this.distance = distance;
    }

    /**
     * Returns the KV object for this search result.
     *
     * @return The KV object for this result.
     */
    public final KvObject<T> getKvObject() {
        return kvObject;
    }

    /**
     * Test whether the result kvObject is an Event.
     *
     * @return true if the result is an Event.
     */
    public final boolean isEvent() {
        return kvObject instanceof Event;
    }

    /**
     * Get the result kvObject as an Event. If the result is not an Event, an
     * IllegalStateException will be thrown.
     *
     * @return the result kvObject as an Event.
     */
    public final Event<T> getEventObject() {
        if(!isEvent()) {
            throw new IllegalStateException("Item is not an event.");
        }
        return (Event<T>)kvObject;
    }

    /**
     * Returns the score of this search results.
     *
     * @return The score for this result.
     */
    public final double getScore() {
        return score;
    }

    /**
     * The distance from the geo coordinate in the query of this search result.
     * The units of value for this data are the same as the units used in the
     * query. This value will only be present when a sort on distance is
     * combined with the geospatial query.
     *
     * @return The distance for this result.
     */
    @Nullable
    public final Double getDistance() {
        return distance;
    }

}
