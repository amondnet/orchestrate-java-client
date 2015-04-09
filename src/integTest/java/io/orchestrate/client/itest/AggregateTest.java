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
package io.orchestrate.client.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.orchestrate.client.Aggregate;
import io.orchestrate.client.AggregateResult;
import io.orchestrate.client.DistanceAggregateResult;
import io.orchestrate.client.Range;
import io.orchestrate.client.RangeAggregateResult;
import io.orchestrate.client.RangeBucket;
import io.orchestrate.client.SearchResults;
import io.orchestrate.client.StatsAggregateResult;
import io.orchestrate.client.TimeInterval;
import io.orchestrate.client.TimeSeriesAggregateResult;
import io.orchestrate.client.TimeSeriesBucket;
import io.orchestrate.client.TopValuesAggregateResult;
import io.orchestrate.client.CountedValue;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

/**
 * {@link io.orchestrate.client.OrchestrateClient#searchCollection(String)}.
 */
public final class AggregateTest extends BaseClientTest {

    @Test
    public void testTopValuesAggregate() throws InterruptedException {
        insertItem("key0", "{`a`:1.0}");
        insertItem("key1", "{`a`:1.0}");
        insertItem("key2", "{`a`:1.0}");
        insertItem("key3", "{`a`:`monkey`}");
        insertItem("key4", "{`a`:`monkey`}");
        insertItem("key5", "{`a`:`monkey`}");
        insertItem("key6", "{`a`:`monkey`}");
        insertItem("key7", "{`a`:true}");
        insertItem("key8", "{`a`:null}");
        insertItem("key9", "{`a`:null}");

        final AggregateResult aggregate = searchForAgg(Aggregate.builder().topValues("value.a"));

        TopValuesAggregateResult topValues = assertAggregate(aggregate, TopValuesAggregateResult.class, "top_values", "value.a", 10);
        List<CountedValue> entries = topValues.getEntries();

        // Check the number of entries in this paged result set
        assertEquals(4, entries.size());
        // Check the values themselves
        assertCountedValueEquals(entries.get(0), "monkey", 4);
        assertCountedValueEquals(entries.get(1), 1.0, 3);
        assertCountedValueEquals(entries.get(2), null, 2);
        assertCountedValueEquals(entries.get(3), false, 1);
    }

    @Test
    public void testTopValuesAggregateWithOffsetAndLimit() throws InterruptedException {
        insertItem("key0", "{`a`:1.0}");
        insertItem("key1", "{`a`:1.0}");
        insertItem("key2", "{`a`:1.0}");
        insertItem("key3", "{`a`:`monkey`}");
        insertItem("key4", "{`a`:`monkey`}");
        insertItem("key5", "{`a`:`monkey`}");
        insertItem("key6", "{`a`:`monkey`}");
        insertItem("key7", "{`a`:true}");
        insertItem("key8", "{`a`:null}");
        insertItem("key9", "{`a`:null}");

        AggregateResult aggregate = searchForAgg(Aggregate.builder().topValues("value.a", 1, 2));

        TopValuesAggregateResult topValues = assertAggregate(aggregate, TopValuesAggregateResult.class, "top_values", "value.a", 10);
        List<CountedValue> entries = topValues.getEntries();

        // Check the number of entries in this paged result set
        assertEquals(2, entries.size());
        // Check the values themselves
        assertCountedValueEquals(entries.get(0), 1.0, 3);
        assertCountedValueEquals(entries.get(1), null, 2);
    }

    @Test
    public void testStatsAggregate() throws InterruptedException {
        insertItem("key1", "{`a`:1.0}");
        insertItem("key2", "{`a`:2.0}");
        insertItem("key3", "{`a`:3.0}");
        insertItem("key4", "{`a`:4.0}");
        insertItem("key5", "{`a`:5.0}");

        AggregateResult aggregate = searchForAgg(Aggregate.builder().stats("value.a"));

        StatsAggregateResult stats = assertAggregate(aggregate, StatsAggregateResult.class, "stats", "value.a", 5);

        assertEquals(1.0, stats.getMin(), 0.0);
        assertEquals(3.0, stats.getMean(), 0.0);
        assertEquals(5.0, stats.getMax(), 0.0);
        assertEquals(15.0, stats.getSum(), 0.0);
        assertEquals(55.0, stats.getSumOfSquares(), 0.0);
        assertEquals(2.0, stats.getVariance(), 0.0001);
        assertEquals(Math.sqrt(2.0), stats.getStdDev(), 0.0001);
    }

    @Test
    public void testRangeAggregate() throws InterruptedException {
        insertItem("key1", "{`a`:1.0}");
        insertItem("key2", "{`a`:2.0}");
        insertItem("key3", "{`a`:3.0}");
        insertItem("key4", "{`a`:4.0}");
        insertItem("key5", "{`a`:5.0}");

        AggregateResult aggregate = searchForAgg(Aggregate.builder().range(
                "value.a",
                Range.below(-10),
                Range.between(-10, 0),
                Range.between(0, 2),
                Range.between(2, 4),
                Range.between(4, 10),
                Range.above(10)
        ));

        RangeAggregateResult range = assertAggregate(aggregate, RangeAggregateResult.class, "range", "value.a", 5);

        List<RangeBucket> buckets = range.getBuckets();
        assertRangeBucketEquals(buckets.get(0), Double.NEGATIVE_INFINITY, -10.0, 0L);
        assertRangeBucketEquals(buckets.get(1), -10.0, 0.0, 0L);
        assertRangeBucketEquals(buckets.get(2), 0.0, 2.0, 1L);
        assertRangeBucketEquals(buckets.get(3), 2.0, 4.0, 2L);
        assertRangeBucketEquals(buckets.get(4), 4.0, 10.0, 2L);
        assertRangeBucketEquals(buckets.get(5), 10.0, Double.POSITIVE_INFINITY, 0L);
    }

    @Test
    public void testDistanceAggregate() throws InterruptedException {
        // Each degree of longitude is equal to about 111.32 km at the equator
        insertItem("key1", "{`lat`:0.0,`lon`:0.0}");
        insertItem("key2", "{`lat`:0.0,`lon`:1.0}");
        insertItem("key3", "{`lat`:0.0,`lon`:-1.0}");
        insertItem("key4", "{`lat`:0.0,`lon`:2.0}");
        insertItem("key5", "{`lat`:0.0,`lon`:-2.0}");
        insertItem("key6", "{`lat`:0.0,`lon`:3.0}");
        insertItem("key7", "{`lat`:0.0,`lon`:-3.0}");

        AggregateResult aggregate = searchForAgg("value:NEAR:{lat:0 lon:0 dist:500km}", Aggregate.builder().distance(
              "value",
              Range.between(0, 112),
              Range.between(112, 224),
              Range.above(224)
        ));

        DistanceAggregateResult distance = assertAggregate(aggregate, DistanceAggregateResult.class, "distance", "value", 7);

        List<RangeBucket> buckets = distance.getBuckets();
        assertRangeBucketEquals(buckets.get(0), 0.0, 112.0, 3L);
        assertRangeBucketEquals(buckets.get(1), 112.0, 224.0, 2L);
        assertRangeBucketEquals(buckets.get(2), 224.0, Double.POSITIVE_INFINITY, 2L);
    }

    @Test
    public void testTimeSeriesDayAggregate() throws InterruptedException {
        insertItem("key1", "{`some_date`:`2014-12-01T05:15:21.123Z`}");
        insertItem("key2", "{`some_date`:`2014-12-02T07:55:19.433Z`}");
        insertItem("key3", "{`some_date`:`2014-12-02T18:48:35.909Z`}");
        insertItem("key4", "{`some_date`:`2014-12-03T12:01:21.451Z`}");
        insertItem("key5", "{`some_date`:`2014-12-04T16:40:56.202Z`}");
        insertItem("key6", "{`some_date`:`2014-12-04T18:33:36.555Z`}");
        insertItem("key7", "{`some_date`:`2014-12-04T22:20:04.753Z`}");

        AggregateResult aggregate = searchForAgg(Aggregate.builder()
              .timeSeries("value.some_date", TimeInterval.DAY)
        );

        TimeSeriesAggregateResult timeSeries = assertAggregate(aggregate, TimeSeriesAggregateResult.class, "time_series", "value.some_date", 7);

        assertEquals(timeSeries.getInterval(), TimeInterval.DAY);

        List<TimeSeriesBucket> buckets = timeSeries.getBuckets();
        assertTimeSeriesBucketEquals(buckets.get(0), "2014-12-01", 1L);
        assertTimeSeriesBucketEquals(buckets.get(1), "2014-12-02", 2L);
        assertTimeSeriesBucketEquals(buckets.get(2), "2014-12-03", 1L);
        assertTimeSeriesBucketEquals(buckets.get(3), "2014-12-04", 3L);
    }

    @Test
    public void testTimeSeriesDayAggregateWithTimeZone() throws InterruptedException {
        insertItem("key1", "{`some_date`:`2014-12-01T05:15:21.123Z`}");
        insertItem("key2", "{`some_date`:`2014-12-02T07:55:19.433Z`}");
        insertItem("key3", "{`some_date`:`2014-12-02T18:48:35.909Z`}");
        insertItem("key4", "{`some_date`:`2014-12-03T12:01:21.451Z`}");
        insertItem("key5", "{`some_date`:`2014-12-04T16:40:56.202Z`}");
        insertItem("key6", "{`some_date`:`2014-12-04T18:33:36.555Z`}");
        insertItem("key7", "{`some_date`:`2014-12-04T22:20:04.753Z`}");

        AggregateResult aggregate = searchForAgg(Aggregate.builder()
                .timeSeries("value.some_date", TimeInterval.DAY, "+1100")
        );

        TimeSeriesAggregateResult timeSeries = assertAggregate(aggregate, TimeSeriesAggregateResult.class, "time_series", "value.some_date", 7);

        assertEquals(timeSeries.getInterval(), TimeInterval.DAY);
        assertEquals(timeSeries.getTimeZone(), "+1100");

        List<TimeSeriesBucket> buckets = timeSeries.getBuckets();
        assertTimeSeriesBucketEquals(buckets.get(0), "2014-12-01", 1L);
        assertTimeSeriesBucketEquals(buckets.get(1), "2014-12-02", 1L);
        assertTimeSeriesBucketEquals(buckets.get(2), "2014-12-03", 2L);
        assertTimeSeriesBucketEquals(buckets.get(3), "2014-12-05", 3L);
    }

    private static void assertTimeSeriesBucketEquals(TimeSeriesBucket bucket, String value, long count) {
        assertEquals(value, bucket.getBucket());
        assertEquals(count, bucket.getCount());
    }

    private static void assertRangeBucketEquals(RangeBucket bucket, double min, double max, long count) {
        assertEquals(min, bucket.getMin(), 0.0);
        assertEquals(max, bucket.getMax(), 0.0);
        assertEquals(count, bucket.getCount());
    }

    private void assertCountedValueEquals(CountedValue countedValue, Object expectedValue, long expectedCount) {
        assertEquals(expectedValue, countedValue.getValue());
        assertEquals(expectedCount, countedValue.getCount());
    }

    public <T extends AggregateResult> T assertAggregate(AggregateResult result, Class<T> expectedClazz, String expectedKind, String expectedField, long expectedValCount) {
        assertEquals(result.getAggregateKind(), expectedKind);
        assertEquals(result.getFieldName(), expectedField);
        assertEquals(result.getValueCount(), expectedValCount);
        return expectedClazz.cast(result);
    }

    private AggregateResult searchForAgg(Aggregate aggQuery) throws InterruptedException {
        return searchForAgg("*", aggQuery);
    }

    private AggregateResult searchForAgg(String searchQuery, Aggregate aggQuery) throws InterruptedException {
        // give time for the writes to hit the search index
        Thread.sleep(1000);

        SearchResults<String> results = client.searchCollection(collection())
                .offset(0).limit(0)
                .aggregate(aggQuery.build())
                .get(String.class, searchQuery)
                .get();
        assertNotNull(results);
        assertNotNull(results.getAggregates());
        Iterator<AggregateResult> aggIter = results.getAggregates().iterator();
        assertNotNull(aggIter);
        assertTrue(aggIter.hasNext());

        AggregateResult firstAggResult = aggIter.next();

        assertNotNull(firstAggResult);

        return firstAggResult;
    }

}
