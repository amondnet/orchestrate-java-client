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
import io.orchestrate.client.KvMetadata;
import io.orchestrate.client.Range;
import io.orchestrate.client.RangeAggregateResult;
import io.orchestrate.client.RangeBucket;
import io.orchestrate.client.SearchResults;
import io.orchestrate.client.StatsAggregateResult;
import io.orchestrate.client.TimeInterval;
import io.orchestrate.client.TimeSeriesAggregateResult;
import io.orchestrate.client.TimeSeriesBucket;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

/**
 * {@link io.orchestrate.client.OrchestrateClient#searchCollection(String)}.
 */
public final class AggregateTest extends BaseClientTest {

    @Test
    public void testStatsAggregate() throws InterruptedException {
        KvMetadata kvMetadata;
        kvMetadata = client.kv(collection(), "key1").put("{\"a\":1.0}").get();
        kvMetadata = client.kv(collection(), "key2").put("{\"a\":2.0}").get();
        kvMetadata = client.kv(collection(), "key3").put("{\"a\":3.0}").get();
        kvMetadata = client.kv(collection(), "key4").put("{\"a\":4.0}").get();
        kvMetadata = client.kv(collection(), "key5").put("{\"a\":5.0}").get();

        // give time for the writes to hit the search index
        Thread.sleep(1000);

        final SearchResults<String> results =
                client.searchCollection(kvMetadata.getCollection())
                      .offset(0).limit(0)
                      .aggregate(Aggregate.builder().stats("value.a").build())
                      .get(String.class, "*")
                      .get();

        assertNotNull(results);
        assertNotNull(results.getAggregates());
        assertNotNull(results.getAggregates().iterator());
        assertTrue(results.getAggregates().iterator().hasNext());

        Iterator<AggregateResult> i = results.getAggregates().iterator();
        AggregateResult aggregate = i.next();

        assertNotNull(aggregate);
        assertTrue(aggregate instanceof StatsAggregateResult);
        StatsAggregateResult stats = (StatsAggregateResult) aggregate;

        assertEquals(stats.getAggregateKind(), "stats");
        assertEquals(stats.getFieldName(), "value.a");
        assertEquals(stats.getValueCount(), 5L);

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
        KvMetadata kvMetadata;
        kvMetadata = client.kv(collection(), "key1").put("{\"a\":1.0}").get();
        kvMetadata = client.kv(collection(), "key2").put("{\"a\":2.0}").get();
        kvMetadata = client.kv(collection(), "key3").put("{\"a\":3.0}").get();
        kvMetadata = client.kv(collection(), "key4").put("{\"a\":4.0}").get();
        kvMetadata = client.kv(collection(), "key5").put("{\"a\":5.0}").get();

        // give time for the writes to hit the search index
        Thread.sleep(1000);

        final SearchResults<String> results =
                client.searchCollection(kvMetadata.getCollection())
                      .offset(0).limit(0)
                      .aggregate(Aggregate.builder().range(
                          "value.a",
                          Range.below(-10),
                          Range.between(-10, 0),
                          Range.between(0, 2),
                          Range.between(2, 4),
                          Range.between(4, 10),
                          Range.above(10)
                      ).build())
                      .get(String.class, "*")
                      .get();

        assertNotNull(results);
        assertNotNull(results.getAggregates());
        assertNotNull(results.getAggregates().iterator());
        assertTrue(results.getAggregates().iterator().hasNext());

        Iterator<AggregateResult> i = results.getAggregates().iterator();
        AggregateResult aggregate = i.next();

        assertNotNull(aggregate);
        assertTrue(aggregate instanceof RangeAggregateResult);
        RangeAggregateResult range = (RangeAggregateResult) aggregate;

        assertEquals(range.getAggregateKind(), "range");
        assertEquals(range.getFieldName(), "value.a");
        assertEquals(range.getValueCount(), 5L);

        List<RangeBucket> buckets = range.getBuckets();
        assertRangeBucketEquals(buckets.get(0), Double.NEGATIVE_INFINITY, -10.0, 0L);
        assertRangeBucketEquals(buckets.get(1), -10.0, 0.0, 0L);
        assertRangeBucketEquals(buckets.get(2), 0.0, 2.0, 1L);
        assertRangeBucketEquals(buckets.get(3), 2.0, 4.0, 2L);
        assertRangeBucketEquals(buckets.get(4), 4.0, 10.0, 2L);
        assertRangeBucketEquals(buckets.get(5), 10.0, Double.POSITIVE_INFINITY, 0L);
    }

    private static final void assertRangeBucketEquals(RangeBucket bucket, double min, double max, long count) {
        assertEquals(min, bucket.getMin(), 0.0);
        assertEquals(max, bucket.getMax(), 0.0);
        assertEquals(count, bucket.getCount());
    }

    @Test
    public void testDistanceAggregate() throws InterruptedException {
        KvMetadata kvMetadata;
        // Each degree of longitude is equal to about 111.32 km at the equator
        kvMetadata = client.kv(collection(), "key1").put("{\"lat\":0.0,\"lon\":0.0}").get();
        kvMetadata = client.kv(collection(), "key2").put("{\"lat\":0.0,\"lon\":1.0}").get();
        kvMetadata = client.kv(collection(), "key3").put("{\"lat\":0.0,\"lon\":-1.0}").get();
        kvMetadata = client.kv(collection(), "key4").put("{\"lat\":0.0,\"lon\":2.0}").get();
        kvMetadata = client.kv(collection(), "key5").put("{\"lat\":0.0,\"lon\":-2.0}").get();
        kvMetadata = client.kv(collection(), "key6").put("{\"lat\":0.0,\"lon\":3.0}").get();
        kvMetadata = client.kv(collection(), "key7").put("{\"lat\":0.0,\"lon\":-3.0}").get();

        // give time for the writes to hit the search index
        Thread.sleep(1000);

        final SearchResults<String> results =
                client.searchCollection(kvMetadata.getCollection())
                      .offset(0).limit(0)
                      .aggregate(Aggregate.builder().distance(
                          "value",
                          Range.below(-10),
                          Range.between(-10, 0),
                          Range.between(0, 2),
                          Range.between(2, 4),
                          Range.between(4, 10),
                          Range.above(10)
                      ).build())
                      .get(String.class, "value:NEAR:{lat:0 lon:0 dist:500km }")
                      .get();

        assertNotNull(results);
        assertNotNull(results.getAggregates());
        assertNotNull(results.getAggregates().iterator());
        assertTrue(results.getAggregates().iterator().hasNext());

        Iterator<AggregateResult> i = results.getAggregates().iterator();
        AggregateResult aggregate = i.next();

        assertNotNull(aggregate);
        assertTrue(aggregate instanceof DistanceAggregateResult);
        DistanceAggregateResult distance = (DistanceAggregateResult) aggregate;

        assertEquals("distance", distance.getAggregateKind());
        assertEquals("value", distance.getFieldName());
        assertEquals(7L, distance.getValueCount());

        List<RangeBucket> buckets = distance.getBuckets();
        assertRangeBucketEquals(buckets.get(0), 0.0, 112.0, 3L);
        assertRangeBucketEquals(buckets.get(1), 112.0, 224.0, 2L);
        assertRangeBucketEquals(buckets.get(2), 224.0, Double.POSITIVE_INFINITY, 2L);
    }

    @Test
    public void testTimeSeriesDayAggregate() throws InterruptedException {
        KvMetadata kvMetadata;
        // Each degree of longitude is equal to about 111.32 km at the equator
        kvMetadata = client.kv(collection(), "key1").put("{\"some_date\":\"2014-12-01T05:15:21.123Z\"}").get();
        kvMetadata = client.kv(collection(), "key2").put("{\"some_date\":\"2014-12-02T07:55:19.433Z\"}").get();
        kvMetadata = client.kv(collection(), "key3").put("{\"some_date\":\"2014-12-02T18:48:35.909Z\"}").get();
        kvMetadata = client.kv(collection(), "key4").put("{\"some_date\":\"2014-12-03T12:01:21.451Z\"}").get();
        kvMetadata = client.kv(collection(), "key5").put("{\"some_date\":\"2014-12-04T16:40:56.202Z\"}").get();
        kvMetadata = client.kv(collection(), "key6").put("{\"some_date\":\"2014-12-04T18:33:36.555Z\"}").get();
        kvMetadata = client.kv(collection(), "key7").put("{\"some_date\":\"2014-12-04T22:20:04.753Z\"}").get();

        // give time for the writes to hit the search index
        Thread.sleep(1000);

        final SearchResults<String> results =
                client.searchCollection(kvMetadata.getCollection())
                      .aggregate(Aggregate.builder()
                          .timeSeries("value.some_date", TimeInterval.DAY)
                          .build()
                      )
                      .offset(0).limit(0)
                      .get(String.class, "*")
                      .get();

        assertNotNull(results);
        assertNotNull(results.getAggregates());
        assertNotNull(results.getAggregates().iterator());
        assertTrue(results.getAggregates().iterator().hasNext());

        Iterator<AggregateResult> i = results.getAggregates().iterator();
        AggregateResult aggregate = i.next();

        assertNotNull(aggregate);
        assertTrue(aggregate instanceof TimeSeriesAggregateResult);
        TimeSeriesAggregateResult timeSeries = (TimeSeriesAggregateResult) aggregate;

        assertEquals(timeSeries.getAggregateKind(), "time_series");
        assertEquals(timeSeries.getFieldName(), "value.some_date");
        assertEquals(timeSeries.getValueCount(), 7L);
        //assertEquals(timeSeries.getInterval(), "day");

        List<TimeSeriesBucket> buckets = timeSeries.getBuckets();
        assertTimeSeriesBucketEquals(buckets.get(0), "2014-12-01", 1L);
        assertTimeSeriesBucketEquals(buckets.get(1), "2014-12-02", 2L);
        assertTimeSeriesBucketEquals(buckets.get(2), "2014-12-03", 1L);
        assertTimeSeriesBucketEquals(buckets.get(3), "2014-12-04", 3L);
    }

    private static final void assertTimeSeriesBucketEquals(TimeSeriesBucket bucket, String value, long count) {
        assertEquals(value, bucket.getBucket());
        assertEquals(count, bucket.getCount());
    }

}
