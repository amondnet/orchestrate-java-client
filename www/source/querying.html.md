The client API is designed around the concept of operations you can execute on
 the Orchestrate.io service. The client library is entirely _asynchronous_.

Under the hood the client makes `HTTP` requests to the [REST API](http://docs.orchestrate.io/).
 All data is written to the platform as [`JSON`](http://json.org/) and the client
 will handle marshalling and unmarshalling the data into Java objects.

Every Key/Value object has a unique identifier that represents the current
 version of the object, this information is known as the "ref". The ref is a
 content-based hash that identifies the specific version of a value. With
 refs, you can track the history of an object and retrieve old versions.

### <a name="constructing-a-client"></a> Constructing a Client

A `Client` object is the starting point for making requests to the
 Orchestrate.io service, it manages the connection pool and makes `HTTP`
 requests to the [REST API](http://docs.orchestrate.io/).

You construct a client using the `API key` for your `Application` which can be
 found in the [Dashboard](https://dashboard.orchestrate.io/) (for help, see
 [here](/#getting-started)).

```java
// An API key looks something like:
//   3854bbd7-0a31-43b0-aa94-66236847a717
Client client = new OrchestrateClient("your api key");
```

#### <a name="multi-data-center"></a> Choosing A Data Center

By default, the Orchestrate client uses 'AWS US East' as its host data center. To use
 another data center, for example 'AWS EU West', you can switch the host when creating
 the client:

```java
Client client = OrchestrateClient.builder("your api key")
    .host("https://api.aws-eu-west-1.orchestrate.io")
    .build();
```

For more information on Orchestrate's Multi Data Center features check out
 [the documentation](http://orchestrate.io/docs/multi-data-center).

A `Client` can be shared across threads and you only need to construct one per
 Java application. For more advanced configuration options, check out
 [Tuning the Client](/advanced-options.html#tuning).

### Stopping a Client

Sometimes it's necessary to release the resources used by a `Client` and
 reconstruct it again at some later stage.

```java
client.close();

// some method later, #get() a request will reallocate resources
client.kv("someCollection", "someKey").get(String.class).get();
```

## <a name="a-sync-api"></a> Blocking vs Non-Blocking API

Any Resource method that returns an OrchestrateRequest will initiate an asynchronous
 http request to the Orchestrate service. For example:

```java
OrchestrateRequest request = client.kv("someCollection", "someKey").get(String.class)
```

The get(Class) method will return an OrchestrateRequest that has been initiated
 asynchronously to the Orchestrate service. To handle the result, you will need to either
 register a listener on that request, or block waiting for the response by calling the
 `get` method on the request. This is what a typical non-blocking call might look like:

```java
client.kv("someCollection", "someKey")
      .get(DomainObject.class)
      .on(new ResponseAdapter<KvObject<DomainObject>>() {
          @Override
          public void onFailure(final Throwable error) {
              // handle error condition
          }

          @Override
          public void onSuccess(final DomainObject object) {
              // do something with the result
          }
      });
```

A blocking call:

```java
DomainObject object = client.kv("someCollection", "someKey")
      .get(DomainObject.class)
      .get();
```

The final `get()` call will block until the result is returned. It takes an optional timeout
 and defaults to 2.5 seconds.

You can also add listeners, even if you ultimately call `get()` to block waiting for the
result:

```java
DomainObject object = client.kv("someCollection", "someKey")
      .get(DomainObject.class)
      .on(new ResponseAdapter<KvObject<DomainObject>>() {
          @Override
          public void onFailure(final Throwable error) {
              // handle error condition
          }

          @Override
          public void onSuccess(final DomainObject object) {
              // do something with the result
          }
      })
      .get();
```

## <a name="key-value"></a> Key-Value

Key-Value operations are the heart of the Orchestrate.io service. These are the
 most common operations you'll perform and is the primary way of storing data.

All Key-Value operations happen in the context of a `Collection`. If the
 collection does not exist it will be _implicitly_ created when data is first
 written.

As mentioned above, all client operations are _asynchronous_.

### <a name="fetch-data"></a> Fetch Data

To fetch an object from a `collection` with a given `key`.

```java
KvObject<DomainObject> object =
        client.kv("someCollection", "someKey")
              .get(DomainObject.class)
              .get();

// check the data exists
if (object == null) {
    System.out.println("'someKey' does not exist.";
} else {
    DomainObject data = kvObject.getValue();
    // do something with the 'data'
}
```

This example shows how to retrieve the value for a key from a collection and
 deserialize the result JSON to a [POJO](http://en.wikipedia.org/wiki/Plain_Old_Java_Object)
 called `object`.

### <a name="list-data"></a> List Data

To list objects in a `collection`.

```java
KvList<DomainObject> results =
        client.listCollection("someCollection")
              .get(DomainObject.class)
              .get();

for (KvObject<DomainObject> kvObject : results) {
    // do something with the object
    System.out.println(kvObject);
}
```

By default, only the first 10 objects are retrieved. This can be increased up to
 100 per request in `KvListResource#limit(int)` method.

The `KvList` object returns a `next` field with a prepared request with the next
 group of objects (or `null` if there are no more objects), this can be used to
  paginate through the collection.

It is also possible to retrieve a list of KV objects without their values by
 setting the `withValues(boolean)` method as the request is being built.

```java
KvList<DomainObject> results =
        client.listCollection("someCollection")
              .withValues(Boolean.FALSE)
              .get(DomainObject.class)
              .get();
```

### <a name="store-data"></a> Store Data

To store (add OR update) an object to a `collection` and a given `key`.

```java
// create some data to store
DomainObject obj = new DomainObject(); // a POJO

final KvMetadata kvMetadata =
        client.kv("someCollection", "someKey")
              .put(obj)
              .get();

// print the 'ref' for the stored data
System.out.println(kvMetadata.getRef());
```

This example shows how to store a value for a key to a collection. `obj` is
 serialized to JSON by the client before writing the data. If the key already
 existed in the collection, then this operation will replace the previous
 version of the item (the old version is still accessible via the 'ref' of that
 version <a href=#refs).

The `KvMetadata` returned by the store operation contains information about
 where the information has been stored and the version (`ref`) it's been written
 with.

### <a name="fetch-data-by-ref"></a> Fetch Data by Ref

To fetch an object from a `collection` with a given `key` and specific `ref`. This
is useful for fetching old versions of an Item.

```java
KvObject<DomainObject> object =
        client.kv("someCollection", "someKey")
              .get(DomainObject.class, "someRef")
              .get();

DomainObject data = kvObject.getValue();
// do something with the 'data'
```

#### <a name="conditional-store"></a> Conditional Store

The `ref` metadata returned from a store operation is important, it allows
 you to perform a "Conditional PUT".

```java
// update 'myObj' if the 'currentRef' matches the ref on the server
KvMetadata kvMetadata =
        client.kv("someCollection", "someKey")
              .ifMatch("someRef")
              .put(obj)
              .get();

// store the new 'obj' data if 'someKey' does not already exist
KvMetadata kvMetadata =
        client.kv("someCollection", "someKey")
              .ifAbsent()
              .put(obj)
              .get();
```

This type of store operation is very useful in high write concurrency
 environments. It provides a pre-condition that must be `true` for the store
 operation to succeed.

#### <a name="server-generated-keys"></a> Store with Server-Generated Keys

With some types of data you'll store to Orchestrate you may want to have the
 service generate keys for the values for you. This is similar to using the
 `AUTO_INCREMENT` feature from other databases.

To store a value to a collection with a server-generated key:

```java
KvMetadata kvMetadata = client.postValue("someCollection", obj).get();
```

### <a name="delete-data"></a> Delete Data

To delete a `collection` of objects.

```java
boolean result =
        client.deleteCollection(collection)
              .get();

if (result) {
    System.out.println("Successfully deleted the collection.");
}
```

To delete an object by `key` in a `collection`.

```java
boolean result =
        client.kv("someCollection", "someKey")
              .delete()
              .get();

if (result) {
    System.out.println("Successfully deleted the collection.");
}
```

#### <a name="conditional-delete"></a> Conditional Delete

Similar to a [conditional store](#conditional-store) operation, a conditional
 delete provides a pre-condition that must be `true` for the operation to
 succeed.

```java
String currentRef = kvMetadata.getRef();
boolean result =
        client.kv("someCollection", "someKey")
              .ifMatch(currentRef)
              .delete()
              .get();

// same as above
```

The object with the key `someKey` will be deleted if and only if the
 `currentRef` matches the current ref for the object on the server.

#### <a name="purge-kv-data"></a> Purge Data

The Orchestrate service is built on the principle that all data is immutable,
 every change made to an object is stored as a new object with a different "ref".
 This "ref history" is maintained even after an object has been deleted, it makes
 it possible to recover deleted objects easily and rollback to an earlier version
 of the object.

Nevertheless there will be times when you may need to delete an object and purge
 all "ref history" for the object.

```java
boolean result =
        client.kv("someCollection", "someKey")
              .delete(Boolean.TRUE)
              .get();

if (result) {
    System.out.println("Successfully purged the key.");
}
```

## <a name="search"></a> Search

A powerful feature of the Orchestrate.io service is the search functionality;
 when an object is written to the platform the data will be semantically indexed
 in the background.

This allows you to perform search queries on the data without any extra
 configuration hassle and no need to run a separate search cluster of some kind
 to ask questions about the data stored.

The query language used to perform searches is the familiar
 [Lucene Syntax](http://lucene.apache.org/core/4_3_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#Overview),
 any Lucene query is a valid Orchestrate.io search query.

The simplest search query is a `*` query.

```java
String luceneQuery = "*";
SearchResults<DomainObject> results =
        client.searchCollection("someCollection")
              .get(DomainObject.class, luceneQuery)
              .get();

for (Result<DomainObject> result : results) {
    // do something with the search results
    System.out.println(result.getScore());
}
```

A more complex search query could look like this:

```java
String luceneQuery = "*";
SearchResults<DomainObject> results =
        client.searchCollection("someCollection")
              .limit(50)
              .offset(10)
              .get(DomainObject.class, luceneQuery)
              .get();

// same as above
```

The collection called `someCollection` will be searched with the query `*` and
 up to `50` results may be returned with a starting offset of `10` from the most
 relevant. The results will be deserialized to `DomainObject`s.

In some cases, it may be helpful to only retrieve the matching keys (and refs).
In this case, use 'withValues(Boolean.FALSE)' to indicate that the item values
should not be included in the response.

```java
String luceneQuery = "*";
SearchResults<DomainObject> results =
        client.searchCollection("someCollection")
              .limit(50)
              .offset(10)
              .withValues(Boolean.FALSE)
              .get(DomainObject.class, luceneQuery)
              .get();

// same as above
```

### Note

Search results are currently limited to no more than __100__ results for each
 query, if this limit is not suitable for you please let us know.

By default, a search operation will only return up to __10__ results, use the
 `CollectionSearchResource` as shown above to retrieve more results for a query.

### Some Example Queries

Here are some query examples demonstrating the Lucene query syntax.

```java
// keyword matching
String luceneQuery = "title:\"foo bar\"";
SearchResults<DomainObject> results =
        client.searchCollection("someCollection")
              .get(DomainObject.class, luceneQuery)
              .get();

// proximity matching
String luceneQuery = "\"foo bar\"~4";
SearchResults<DomainObject> results =
        client.searchCollection("someCollection")
              .get(DomainObject.class, luceneQuery)
              .get();

// range searches
String luceneQuery = "year_of_birth:[20020101 TO 20030101]";
SearchResults<DomainObject> results =
        client.searchCollection("someCollection")
              .get(DomainObject.class, luceneQuery)
              .get();
```

Ignore the backslashes in the first two examples, this is necessary to escape
 the quotes in the Java string literal.

## <a name="events"></a> Events

In the Orchestrate.io service, an event is a time ordered piece of data you want
 to store in the context of a key. This specialist storage type exists because
 we believe it's pretty common in the design of most applications.

Some examples of types of objects you'd want to store as events are; comments
 that belong to a blog article, items in a user's news feed from a social
 network, or billing history from a customer.

### <a name="fetch-events"></a> Fetch Events

To fetch events belonging to a `key` in a specific `collection` of a specific
 `type`, where type could be a name like "comments" or "feed".

```java
Iterable<Event<DomainObject>> events =
        client.event("someCollection", "someKey")
              .type("eventType")
              .get(DomainObject.class)
              .get();

// iterate on the events, they will be ordered by the most recent value
for (Event<MyObject> event : events) {
    System.out.println(event.getTimestamp());
}
```

You can also supply an optional `start` and `end` timestamp to retrieve a subset
 of the events.

```java
Iterable<Event<DomainObject>> results =
        client.event("someCollection", "someKey")
              .type("eventType")
              .start(0L)
              .end(13865200L)
              .get(DomainObject.class)
              .get();

// same as above
```

### <a name="store-event"></a> Store Event

You can think of storing an event like adding to the front of a time-ordered
 immutable list of objects.

To store an event to a `key` in a `collection` with a specific `type`.

```java
DomainObject obj = new DomainObject(); // a POJO
boolean result =
        client.event("someCollection", "someKey")
              .type("eventType")
              .put(obj)
              .get();

if (result) {
    System.out.println("Successfully stored an event.");
}
```

You can also supply an optional `timestamp` for the event, this will be used
 instead of the timestamp of the write operation.

```java
DomainObject obj = new DomainObject(); // a POJO
boolean result =
        client.event("someCollection", "someKey")
              .type("eventType")
              .put(obj, 13865200L)
              .get();

// same as above
```

## <a name="graph"></a> Graph

While building an application it's possible that you'll want to make associations
 between particular objects of the same type or even objects of completely
 different types that share a property of some kind.

It's this sort of data problem that makes the graph features in Orchestrate
 shine, if you're building a socially-aware application you might want to
 add relations between data like "friend" or "follows".

A graph query is the right choice when you have a starting object that you want
 search from to follow relevant relationships and accumulate interesting
 information.

### <a name="fetch-relations"></a> Fetch Relations

To fetch objects related to the `key` in the `collection` based on a
 relationship or number of `relation`s.

```java
Iterable<KvObject<DomainObject>> results =
        client.relation("someCollection", "someKey")
              .get(DomainObject.class, "someKind")
              .get();

for (KvObject<String> result : results) {
    // the raw JSON string
    System.out.println(result.getValue());
}
```

Imagine that we'd like to know the `follow`ers of `users` that are `friend`s of
 the user `tony`. This kind of query could look like this.

```java
Iterable<KvObject<DomainObject>> results =
        client.relation("someCollection", "someKey")
              .get(DomainObject.class, "friend", "follow")
              .get();

// same as above
```

### <a name="store-relation"></a> Store Relation

To store a `relation` between one `key` to another `key` within the same
 `collection` or across different `collection`s.

```java
boolean result =
        client.relation("sourceCollection", "sourceKey")
              .to("destCollection", "destKey")
              .put("someKind")
              .get();

if (result) {
    System.out.println("Successfully stored the relation.");
}
```

#### Note

Relationships in Orchestrate are uni-directional, to define a bi-directional
 relation you must swap the `collection` <-> `toCollection` and `key` <-> `toKey`
 parameters.

We may lift this restriction in a future release of the client.

### <a name="purge-relation"></a> Purge Relation

To purge a `relation` between one `key` to another `key` within the same
 `collection` or across different `collection`s.

```java
boolean result =
        client.relation("sourceCollection", "sourceKey")
              .to("destCollection", "destKey")
              .purge("someKind")
              .get();

if (result) {
    System.out.println("Successfully purged the relation.");
}
```
