The client API is designed around the concept of operations you can execute on
 the Orchestrate.io service. The client library is entirely _asynchronous_. 
 However, most examples here show the blocking variation, because it makes the
 examples easier to understand. Please see the [Async API](#async-api) section 
 for more details about how the Blocking vs Non-Blocking approaches work.

Under the hood the client makes `HTTP` requests to the [REST API](http://docs.orchestrate.io/).
 All data is written to the platform as [`JSON`](http://json.org/) and the client
 will handle marshalling and unmarshalling the data into Java objects.

Every Key/Value object has metadata associated with it. This is the "path" metadata that 
 Orchestrate associates with the data, and includes in operation responses. The client 
 will parse this metadata and make it available either in KvMetadata objects (for most 
 create, update, and delete operations), or in the KvObject (for query operations). For 
 query operations, the Item's value is also provided as the 'value' property of the KvObject.
 Therefore, most of the responses and response handlers in the client either provide 
 KvMetadata objects or KvObjects.

### <a name="constructing-a-client"></a> [Constructing a Client](#constructing-a-client)

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

#### <a name="multi-data-center"></a> [Choosing A Data Center](#multi-data-center)

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

### <a name="stopping-client"></a> [Stopping a Client](#stopping-client)

Sometimes it's necessary to release the resources used by a `Client` and
 reconstruct it again at some later stage.

```java
client.close();

// some method later, #get() a request will reallocate resources
client.kv("someCollection", "someKey").get(String.class).get();
```

## <a name="key-value"></a> [Key-Value](#key-value)

Key-Value operations are the heart of the Orchestrate.io service. These are the
 most common operations you'll perform and is the primary way of storing data.

All Key-Value operations happen in the context of a `Collection`. If the
 collection does not exist it will be _implicitly_ created when data is first
 written. 
 
For the examples here, we will assume there is a collection called
 `users`. This collection will use emails as the users' keys. We will 
 also map the response to a `User` class, which is a simple POJO class:
 
```java
public class User {
    private String name;
    private String description;

    public User(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public User() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return name.equals(user.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
```


### <a name="fetch-data"></a> [Fetch Data](#fetch-data)

To fetch an object from our `users` `collection` with a given `key`.

```java
KvObject<User> userKv =
        client.kv("users", "test@email.com")
              .get(User.class)  // send the HTTP GET request
              .get();           // block and return the response

// check the data exists
if (userKv == null) {
    System.out.println("User 'test@email.com' does not exist.";
} else {
    User user = userKv.getValue();
    String userEmail = userKv.getKey();
    String ref = userKv.getRef();
    System.out.println(String.format(
      "Read user key:%s, name:%s, ref:%s",
          userKey, user.getName(), ref
    ));
}
```

This example shows how to retrieve the value for a key from a collection and
 deserialize the result JSON to a [POJO](http://en.wikipedia.org/wiki/Plain_Old_Java_Object).
 The User value is wrapped in a KvObject. This allows the client to provide the
 other metadata for the item. In the example, we show that the `key` and the `ref`
 are provided on the KvObject. The `ref` is a content-based hash that Orchestrate
 provides for the Item. This `ref` is used in Orchestrate's versioning history.
 

#### <a name="fetch-data-by-ref"></a> [Fetch Data by Ref](#fetch-data-by-ref)

To fetch an object from a `collection` with a given `key` and specific `ref`. This
is useful for fetching old versions of an Item.

```java
// this dummyRef would likely be provided from an earlier operation
String dummyRef = "a203b02a7d0b6de8";
KvObject<User> userKv =
        client.kv("users", "test@email.com")
              .get(User.class, dummyRef)
              .get();

User user = userKv.getValue();
// do something with the 'user'
```

### <a name="list-data"></a> [List Data](#list-data)

To list objects in a `collection`.

```java
KvList<User> results =
        client.listCollection("users")
              .get(User.class)
              .get();

for (KvObject<User> userKv : results) {
    // do something with the object
    System.out.println(userKv);
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
KvList<User> results =
        client.listCollection("users")
              .withValues(Boolean.FALSE)
              .get(User.class)  // sends the HTTP GET request
              .get();           // blocks and returns the response

for (KvObject<User> userKv : results) {
	// userKv.getValue() will be null here because we 
	// requested withValues(false)
	// other metadata is available though:
	System.out.println(userKv.getKey()+","+userKv.getRef());
	
}
```
In this case, all the metadata will be present on the KvObjects, but the `value`
will be `null` on all of the results.

### <a name="store-data"></a> [Store Data](#store-data)

To store (add OR update) an object to a `collection` and a given `key`.

```java
// create some data to store
User user = new User("Chris", "Likes to code."); // a POJO

final KvMetadata userMeta =
        client.kv("users", "test@test.com")
              .put(user)   // sends the HTTP PUT request
              .get();      // blocks and returns the response

// print the 'ref' for the stored data
System.out.println(userMeta.getRef());
```

This example shows how to store a value for a key to a collection. `user` is
 serialized to JSON by the client before writing the data. If the key already
 existed in the collection, then this operation will replace the previous
 version of the item (the old version is still accessible via the 
 <a href="https://orchestrate.io/docs/apiref#refs">ref</a> of that version).

The `KvMetadata` returned by the store operation contains information about
 where this new version of the item has been stored, including its version
 `ref`.

#### <a name="conditional-store"></a> [Conditional Store](#conditional-store)

The `ref` metadata returned from a store operation is important, it allows
 you to perform a "Conditional PUT".

```java
// update 'user' if the latest version 'ref' on the server matches 
// the provided 'lastRef'
String lastRef = "a203b02a7d0b6de8"; // likely kept from an earlier operation
try {
    KvMetadata updatedUserMeta =
        client.kv("users", "test@test.com")
              .ifMatch(lastRef)
              .put(user)
              .get();
} catch (ItemVersionMismatchException ex) {
   // update failed because the refs do not match. This would usually
   // mean the item was updated since we last read it.
}

```

You may also want to be sure you are inserting a NEW Item, and not unintentionally
updating an Item that was already written for the same key.

```
// store the new 'user' data if the key 'test@test.com' does not already exist
try {
    KvMetadata userMeta =
        client.kv("users", "test@test.com")
              .ifAbsent()
              .put(user)
              .get();
} catch (ItemAlreadyPresentException ex) {
  // the key 'test@test.com' already exists in the collection
}
```

These "conditional store" operations are very useful in high write concurrency
 environments. They provide a pre-condition that must be `true` for the store
 operation to succeed.

#### <a name="server-generated-keys"></a> [Store with Server-Generated Keys](#server-generated-keys)

With some types of data you'll store to Orchestrate you may want to have the
 service generate keys for the values for you. This is similar to using the
 `AUTO_INCREMENT` feature from other databases. Orchestrate's generated keys
 are NOT guaranteed to be monotonically increasing. They are roughly time 
 ordered though, and may be out of order by up to 1s.

To store a value to a collection with a server-generated key:

```java
User user = new User("Chris", "Likes to code."); // a POJO
KvMetadata userMeta = client.postValue("users", user).get();
// print out the generated key
System.out.println(userMeta.getKey);
```

### <a name="partial-update-data"></a> [Partial Update Data](#partial-update-data)

To update only a portion of an item (eg to update a few fields).

```java
final KvMetadata updatedMeta =
    client.kv("users", "test@test.com")
        .patch(JsonPatch.builder()
            .replace("description", "Likes all code.")
            .inc("views")          // increment a 'views' counter
            .add("tags/-","coder") // append to a 'tags' array
            .build()
        )
        .get();

// print the 'ref' for the stored data
System.out.println(updatedMeta.getRef());
```

This operation allows for updating a portion of a document by applying a
 list of operations. Each of the operations will be applied to the specified
 document in order. If any of the operations fails for any reason, then the
 patch is aborted, and none of the changes are applied.
 The response metadata will contain the new 'ref' for the updated item. For
 a full list of supported Ops, see the JsonPatch [Javadocs](/javadoc/latest/io/orchestrate/client/jsonpatch/JsonPatch.html).
.

#### <a name="conditional-partial-update-data"></a> [Conditional Partial Update Data](#conditional-partial-update-data)
This patch operation also supports the ifMatch conditional. When updating
 individual fields, it is recommended that you keep the 'ref' of the item
 you are modifying. Then, when sending the update, provide the original 'ref'
 to insure the updates are being applied to the expected version.

```java
String lastRef = "a203b02a7d0b6de8"; // likely kept from an earlier operation
final KvMetadata updatedMeta =
    client.kv("users", "test@test.com")
        .ifMatch(lastRef)
        .patch(JsonPatch.builder()
            .replace("description", "Likes all code.")
            .build()
        )
        .get();

// print the 'ref' for the stored data
System.out.println(kvMetadata.getRef());
```

#### <a name="test-operation"></a> [Test Operation](#test-operation)
One of the JsonPatch ops available is the 'test' op. This op deserves special
 mention. The 'test' op provides a way to indicate a field value based precondition
 for the patch operation. The operations will be applied to the document in
 the order specified. If any 'test' op fails, the entire patch is aborted, and
 NONE of the ops will be 'committed' to the store.

```java
try {
    final KvMetadata updatedMeta =
        client.kv("users", "test@test.com")
             .patch(JsonPatch.builder()
                 .test("description", "Likes to code.")
                 .replace("description", "Likes ALL code.")
                 .build()
             )
             .get();

    // print the 'ref' for the stored data
    System.out.println(updatedMeta.getRef());
} catch (TestOpApplyException ex) {
   // the patch failed to apply due to a 'test' op failure.
   System.out.println("Test op at index " + ex.getOpIndex() + " failed. Data: "+ex.getDetails().toString());
} catch (PatchConflictException ex) {
   // the patch failed to apply due to one of the 'path's
   // specified in one of the ops does not exist (ie it may have
   // been removed by another update of the item).
   System.out.println("Patch Op at index " + ex.getOpIndex() + " failed. Data: "+ex.getDetails().toString());
}
```

#### <a name="merge-update-data"></a> [Merge Update Data](#merge-update-data)

To update an Item by merging it with another (via a JsonMergePatch
 https://tools.ietf.org/html/rfc7386). 
 
```java
String changesJson = "{\"description\":\"Looking at Rust.\"}"
final KvMetadata updatedMeta =
    client.kv("users", "test@test.com")
        .merge(changesJson)   // send the HTTP PATCH Request
        .get();			      // block and return the meta  

// print the 'ref' for the stored data
System.out.println(updatedMeta.getRef());
```

This operation allows for updating a JSON document by merging it with
another JSON document that contains new values to apply to the original.

The usefulness of this method in the Java client is likely limited to only
command line type utilities that are processing sets of changes
from other sources (otherwise, it is unlikely a normal Java domain object
will be only partially populated). Due to this limited utility, this
method only takes a JSON string. Please provide feedback if your use case
does not fit this assumption.

#### <a name="conditional-merge-update-data"></a> [Conditional Merge Update Data](#conditional-merge-update-data)
This merge operation also supports the ifMatch conditional. When merging
json documents, it is recommended that you keep the 'ref' of the item
you are modifying. Then, when sending the update, provide the original 'ref'
to insure the updates are being applied to the expected version.

```java
String lastRef = "a203b02a7d0b6de8"; // likely kept from an earlier operation
String changesJson = "{\"description\":\"Looking at Elixir.\"}"
final KvMetadata updatedMeta =
    client.kv("users", "test@test.com")
        .ifMatch(lastRef)
        .merge(changesJson)
        .get();

// print the 'ref' for the stored data
System.out.println(updatedMeta.getRef());
```

#### <a name="conditional-update-failures"></a> [Conditional Partial Update Failures](#conditional-update-failures)
When a conditional partial update (either patch or merge) fails, NONE of the operations
are applied. The failure indicates that the precondition failed (ie the item has been
modified since the provided ref). This failure is reflected in the client by an exception
being thrown.

```java
String lastRef = "a203b02a7d0b6de8"; // likely kept from an earlier operation
try {
    final KvMetadata kvMetadata =
        client.kv("users", "test@test.com")
            .ifMatch(lastRef)
            .patch(JsonPatch.builder()
                .replace("description", "Looking at Heartforth.")
                .build()
            )
            .get();
} catch (PatchConflictException ex) {
   // the patch failed to apply due to either a 'test' failure or
   // one of the 'path's specified in one of the ops does not exist
   // (ie it may have been removed by another update of the item).
   System.out.println("Patch Op at index " + ex.getOpIndex() +
        " failed. Data: "+ex.getDetails().toString());
} catch (ItemVersionMismatchException ex) {
   // patch failed to apply because the 'lastRef' does not match
   // the latest version for the key in the collection
}

```

If an item key is updated VERY frequently, then there is a chance that a patch request for
that key could result in a ItemVersionMismatchException, even if it was not sent
with an ifMatch header.

```java
try {
    final KvMetadata kvMetadata =
        client.kv("someCollection", "someKey")
            .patch(JsonPatch.builder()
                .replace("description", "Looking at Purescript.")
                .build()
            )
            .get();
} catch (PatchConflictException ex) {
   // the patch failed to apply due to either a 'test' failure or
   // one of the 'path's specified in one of the ops does not exist
   // (ie it may have been removed by another update of the item).
} catch (ItemVersionMismatchException ex) {
   // since there was NO ifMatch value sent, this means the
   // patch failed to apply due to too many other requests to
   // update the same item key
}

```

### <a name="delete-data"></a> [Delete Data](#delete-data)

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

You can also delete an entire `collection`. This is a very rare operation, since it will
remove all the data in the collection. Deleting a collection is analgous to doing
a `drop table` in a relational database.


```java
boolean result =
        client.deleteCollection(collection) // send DELETE
              .get();                       // block for response

if (result) {
    System.out.println("Successfully deleted the collection.");
}
```

#### <a name="conditional-delete"></a> [Conditional Delete](#conditional-delete)

Similar to a [conditional store](#conditional-store) operation, a conditional
 delete provides a pre-condition that must be `true` for the operation to
 succeed.

```java
String lastRef = "a203b02a7d0b6de8"; // likely kept from an earlier operation
boolean result =
        client.kv("users", "test@test.com")
              .ifMatch(lastRef)
              .delete()
              .get();

if (result) {
    System.out.println("Successfully deleted the item.");
}
```

The object with the key `test@test.com` will be deleted if and only if the
 `lastRef` matches the current ref for the object on the server.

#### <a name="purge-kv-data"></a> [Purge Data](#purge-kv-data)

The Orchestrate service is built on the principle that all data is immutable,
 every change made to an object is stored as a new object with a different `ref`.
 This <a href="https://orchestrate.io/docs/apiref#refs-list">`ref history`</a>
 is maintained even after an object has been deleted, it makes
 it possible to recover deleted objects easily and rollback to an earlier version
 of the object.

Nevertheless there will be times when you may need to delete an object and purge
 all `ref history` for the object.

```java
boolean result =
        client.kv("users", "test@test.com")
              .delete(Boolean.TRUE)
              .get();

if (result) {
    System.out.println("Successfully purged the key.");
}
```

## <a name="search"></a> [Search](#search)

A powerful feature of the Orchestrate.io service is the search functionality;
 when an object is written to the platform the data will be semantically indexed
 in the background.

This allows you to perform search queries on the data without any extra
 configuration hassle and no need to run a separate search cluster of some kind
 to ask questions about the data stored.

The query language used to perform searches is the familiar
 [Lucene Syntax](http://lucene.apache.org/core/4_3_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#Overview),
 any Lucene query is a valid Orchestrate.io search query.

The simplest search query is a `*` query. This is what a query against 
the `users` collection would look like:

```java
String luceneQuery = "*";
SearchResults<User> results =
        client.searchCollection("users")
              .get(User.class, luceneQuery)
              .get();

for (Result<User> result : results) {
    // do something with the search results
    KvObject<User> userKv = result.getKvObject();
    String key = userKv.getKey();
    String ref = userKev.getRef();
    User user = userKv.getValue();
    
    System.out.println(String.format(
      "Found user email:%s, name:%s, ref:%s",
          key, user.getName(), ref
    ));
}
```

A more complex search query could look like this:

```java
String luceneQuery = "description:java";
SearchResults<User> results =
        client.searchCollection("users")
              .limit(50)
              .offset(10)
              .get(User.class, luceneQuery)
              .get();

// same as above
```

The collection called `users` will be searched with the query `description:java`
 (which finds users whose description contains 'java') and return
 up to `50` results, starting at offset `10` (limit and offset are a
 common pagination mechanism). The result values will be deserialized 
 to `User` instances.

In some cases, it may be helpful to only retrieve the matching keys (and refs).
In this case, use 'withValues(Boolean.FALSE)' to indicate that the item values
should not be included in the response.

```java
String luceneQuery = "*";
SearchResults<User> results =
        client.searchCollection("users")
              .limit(50)
              .offset(10)
              .withValues(Boolean.FALSE)
              .get(User.class, luceneQuery)
              .get();

for (Result<User> result : results) {
    // do something with the search results
    KvObject<User> userKv = result.getKvObject();
    String email = userKv.getKey();
    // userKv.getValue() will be null because the search request
    // has "withValues(false)". 
    
    System.out.println(email + ": "+result.getScore());
}
```

#### <a name="query-note"></a> [Note](#query-note)
By default, the search functionality will only search Kv Items. To search events, you must
specify a `path metadata` predicate: `@path.kind:event`. See [Search Events](#search-events)
for more on searching for events.

#### <a name="query-note"></a> [Note](#query-note)

Search results are currently limited to no more than __100__ results for each
 query, if this limit is not suitable for you please let us know.

By default, a search operation will only return up to __10__ results, use the
 `CollectionSearchResource` as shown above to retrieve more results for a query.

#### <a name="query-examples"></a> [Some Example Queries](#query-examples)

Here are some query examples demonstrating the Lucene query syntax.

In these examples, `DomainObject` is just a normal java POJO like the `User`
class in other examples.

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

The backslashes in the first two examples are necessary to escape
 the quotes in the Java string literal, so that the query is sent 
 as a `phrase query`. If the query didn't have the inner quotes
 (for example: "title: foo bar", would be a logical OR, where
 title could have `foo` OR `bar`).  

## <a name="aggregates"></a> [Aggregate Functions](#aggregates)

In the Orchestrate.io search API, any query can be optionally accompanied by a
collection of aggregate functions, each providing a summary of the data items
matched by the query. There are five different kinds of aggregate functions:
TopValues, Statistical, Range, Distance, and TimeSeries.

Here are a few examples to show how to use aggregate functions in common
scenarios. Again, `DomainObject` is just a POJO, just like our previous `User` 
examples.

```java
// TopValues Aggregate: Find the most common values for a field and count the
// number of occurrences of each unique value.
SearchResults<DomainObject> results =
        client.searchCollection("blog_posts")
              .aggregate(Aggregate.builder()
                  .topValues("value.category", 0 , 10)
                  .build()
              )
              .get(DomainObject.class, "*")
              .get();

// Stats Aggregate: Generate a statistical summary of the prices of items in your users' shopping carts
SearchResults<DomainObject> results =
        client.searchCollection("shopping_cart")
              .aggregate(Aggregate.builder()
                  .stats("value.items.price")
                  .build()
              )
              .get(DomainObject.class, "*")
              .get();

// Range Aggregate: Create a range histogram of user ratings for a particular product
String luceneQuery = "value.product_id:`ABC123`";
SearchResults<DomainObject> results =
        client.searchCollection("user_ratings")
              .aggregate(Aggregate.builder()
                  .range(
                      "value.number_of_stars",
                      Range.between(0.0, 1.0),
                      Range.between(1.0, 2.0),
                      Range.between(2.0, 3.0),
                      Range.between(3.0, 4.0),
                      Range.between(4.0, 5.0)
                  )
                  .build()
              )
              .get(DomainObject.class, luceneQuery)
              .get();

// Distance Aggregate: Count the number of Chinese restaurants within various distance ranges (0 - 1 km, 1 - 2 km, etc)
String luceneQuery = "value.cuisine:chinese AND value.location:NEAR:{ lat:12.34 lon:56.78 dist:5km }";
SearchResults<DomainObject> results =
        client.searchCollection("restaurants")
              .aggregate(Aggregate.builder()
                  .distance(
                      "value.location",
                      Range.between(0.0, 1.0),
                      Range.between(1.0, 2.0),
                      Range.between(2.0, 3.0),
                      Range.between(3.0, 4.0),
                      Range.between(4.0, 5.0)
                  )
                  .build()
              )
              .get(DomainObject.class, luceneQuery)
              .get();

// TimeSeries Aggregate: Count the number new users per day, over the past 30 days,
// in my local time zone, "-0800" (Pacific Standard Time is eight hours behind UTC).
String luceneQuery = "value.signup_date:[2014-11-01 TO 2014-12-01]";
SearchResults<DomainObject> results =
        client.searchCollection("users")
              .aggregate(Aggregate.builder()
                  .timeSeries("value.signup_date", TimeInterval.DAY, "-0800")
                  .build()
              )
              .get(DomainObject.class, luceneQuery)
              .get();
```

You can include multiple different aggregate functions in a single query by chaining calls to
the AggregateBuilder. For example, this query includes both a Statistical and Range aggregate
on the same field, as well as two different TimeSeries aggregates, with different intervals and
different fields:

```java
String luceneQuery = "value.performer:radiohead";
SearchResults<DomainObject> results =
        client.searchCollection("concert_tickets")
              .aggregate(Aggregate.builder()
                  .stats("value.sale_price")
                  .range(
                      "value.sale_price"
                      Range.below(25),
                      Range.between(25, 50),
                      Range.between(50, 75),
                      Range.between(75, 100),
                      Range.above(100)
                  )
                  .timeSeries("value.performance_date", TimeInterval.MONTH)
                  .timeSeries("value.transaction_date", TimeInterval.DAY)
                  .build()
              )
              .get(DomainObject.class, luceneQuery)
              .get();
```

## <a name="events"></a> [Events](#events)

In the Orchestrate.io service, an event is a time ordered piece of data you want
 to store in the context of a key. This specialist storage type exists because
 we believe it's pretty common in the design of most applications.

Some examples of types of objects you'd want to store as events are; comments
 that belong to a blog article, items in a user's news feed from a social
 network, or billing history from a customer.
 
For the Event examples, we will assume we have a "users" `collection` and that 
each user `key` has an event called `logs`. We will show the results being mapped to a
`LogItem` class. You would replace that class with one of your own, and it 
just needs to be a normal POJO class (java bean conventions). For
our examples, we'll assume that `LogItem` looks like this:

```java
public class LogItem {
    private String source;
    private String description;

    public LogItem() {
    }

    public LogItem(String source, String description) {
        this.source = source;
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
```


### <a name="fetch-events"></a> [Fetch Events](#fetch-events)

To fetch events belonging to a `key` in a specific `collection` of a specific
 `type`, where type could be a name like "activities", "comments" or "feed".

```java
Iterable<Event<LogItem>> events =
        client.event("users", "test@test.com")
              .type("activities") // the event type name to fetch from
              .get(LogItem.class) // send the GET request, map responses to LogItem instances
              .get();

// iterate on the events, they will be ordered by the most recent value
for (Event<LogItem> event : events) {
    long eventTimestamp = event.getTimestamp();
    LogItem logItem = event.getValue();
    System.out.println(eventTimestamp + ": " + logItem.getDescription());
}
```

You can also supply an optional `start` and `end` timestamp to retrieve a subset
 of the events.

```java
Iterable<Event<LogItem>> results =
        client.event("users", "test@test.com")
              .type("activities")
              .start(0L)
              .end(1369832019085L)
              .get(LogItem.class)
              .get();

// same as above
```

#### <a name="fetch-single-event"></a> [Fetch Single Event](#fetch-single-event)

To fetch an individual event instance.

```java
// here, the timestamp and ordinal are likely from an earlier
// operation response.
Long timestamp = 1369832019085L;
Long ordinal = 612808568709574700L
Event<LogItem> event =
        client.event("users", "test@test.com")
              .type("activities")
              .timestamp(timestamp)
              .ordinal(ordinal)
              .get(LogItem.class)
              .get();

LogItem logItem = event.getValue();
System.out.println(logItem.getDescription());
```

#### <a name="search-events"></a> [Search Events](#search-events)

Events can be searched using the same Search api. To find events, the query must 
include the predicate for finding events: `@path.kind:event`. This `path metadata` predicate
indicates we are only searching for `event` objects. This meta defaults to `item` so that 
ONLY items are returned unless `@path.kind` is specified explicitly. 

By specifying the `@path.kind:event` you are able to search for events across keys 
in a collection.

```java
// @path.kind:event will search only events, and
// @path.type:activities will search only the 'activities' event type
String luceneQuery = "@path.kind:event AND @path.type:activities AND description:website";
SearchResults<LogItem> results =
        client.searchCollection("users")
              .get(LogItem.class, luceneQuery)
              .get();
for(Result<LogItem> result: results) {
    Event<LogItem> logEvent = result.getEventObject();
    String key = logEvent.getKey();
    Long timestamp = logEvent.getTimestamp();
    LogItem logItem = logEvent.getValue();
    
    System.out.println(String.format(
      "Found event key:%s, timestamp:%s, description:%s",
          key, timestamp, logItem.getDescription()
    ));
}
```

There is a convenience method to set the 'kind' metadata predicate for you:

```java
String luceneQuery = "@path.type:activities AND description:website";
SearchResults<LogItem> results =
        client.searchCollection("users")
              .kinds("event")
              .get(LogItem.class, luceneQuery)
              .get();
// same as above
```

You can also Sort by the event `timestamp` metadata, so the results are ordered most
recent first.

```java
// @path.type:activities will search only the 'activities' event type
String luceneQuery = "@path.type:activities AND description:website";
SearchResults<LogItem> results =
        client.searchCollection("users")
              .kinds("event")
              .sort("@path.timestamp:desc")
              .get(LogItem.class, luceneQuery)
              .get();
for(Result<LogItem> result: results) {
    Event<LogItem> logEvent = result.getEventObject();
    String key = logEvent.getKey();
    Long timestamp = logEvent.getTimestamp();
    LogItem logItem = logEvent.getValue();
    
    System.out.println(String.format(
      "Found event key:%s, timestamp:%s, description:%s",
          key, timestamp, logItem.getDescription()
    ));
}
```

Searching across event types works too, but will require a bit more handling since 
the different event types will likely map to different Java Objects.

```java
// find ANY events with description containing "website"
String luceneQuery = "description:website";
SearchResults<Void> results =
        client.searchCollection("users")
              .kinds("event")
              .get(luceneQuery)
              .get();
for(Result<Void> result: results) {
    Event<Void> eventResult = result.getEventObject();
    String eventType = eventResult.getType();
    String key = eventResult.getKey();
    Long timestamp = eventResult.getTimestamp();
    
    if("activities".equals(eventType)) {
      // assuming our app maps 'activity' events to a POJO class called LogItem
      LogItem logItem = eventResult.getValue(LogItem.class);
    
      System.out.println(String.format(
        "Found ACTIVITY: key:%s, timestamp:%s, description:%s",
            key, timestamp, logItem.getDescription()
      ));
    } else if("payments".equals(eventType)) {
      // assuming our app maps 'payment' events to a POJO class called UserPayment
      UserPayment payment = eventResult.getValue(UserPayment.class);
    
      System.out.println(String.format(
        "Found PAYMENT: key:%s, timestamp:%s, description:%s",
            key, timestamp, payment.getDescription()
      ));
    }
}

```

You can also perform a search for KV Items AND Events.

```java
// find ANY KV Items or Events with description containing "website"
String luceneQuery = "description:website";
SearchResults<Void> results =
        client.searchCollection("users")
              .kinds("item", "event")
              .get(luceneQuery)
              .get();
for(Result<Void> result: results) {
    KvObject<Void> kvResult = result.getKvObject();
    String key = kvResult.getKey();
    if (result.isEvent()) {
       Event<Void> eventResult = result.getEventObject();
       String eventType = eventResult.getType();
	    Long timestamp = eventResult.getTimestamp();
    
       if("activities".equals(eventType)) {
         // assuming our app maps 'activity' events to a POJO class called LogItem
         LogItem logItem = eventResult.getValue(LogItem.class);
    
         System.out.println(String.format(
           "Found ACTIVITY: key:%s, timestamp:%s, description:%s",
               key, timestamp, logItem.getDescription()
         ));
       } else if("payments".equals(eventType)) {
         // assuming our app maps 'payment' events to a POJO class called UserPayment
         UserPayment payment = eventResult.getValue(UserPayment.class);
    
         System.out.println(String.format(
           "Found PAYMENT: key:%s, timestamp:%s, description:%s",
               key, timestamp, payment.getDescription()
         ));
       }
    } else {
        User user = kvResult.getValue(User.class);
        System.out.println(String.format(
           "Found USER: key:%s, description:%s",
               key, user.getDescription()
         ));
    }
}

```

### <a name="store-event"></a> [Store Event](#store-event)

You can think of storing an event like adding to the front of a time-ordered
 immutable list of objects.

To store an event to a `key` in a `collection` with a specific `type`.

```java
LogItem logItem = new LogItem("website", "viewed homepage");
EventMetadata result =
        client.event("users", "test@test.com")
              .type("activities")
              .create(logItem)  // send the HTTP POST requst
              .get();           // block waiting for the response

// Print the timestamp and ordinal of the newly created event
System.out.println(result.getTimestamp() + ", "+result.getOrdinal());
```

You can also supply an optional `timestamp` for the event, this will be used
 instead of the timestamp of the write operation. This may be useful if your
 Events already have a timestamp (ie from a log file).

```java
Long timestamp = 1369832019085L;
LogItem logItem = new LogItem("website", "viewed homepage");
EventMetadata result =
        client.event("users", "test@test.com")
              .type("activities")
              .timestamp(timestamp)
              .create(logItem)
              .get();

// Print the timestamp and ordinal of the newly created event
System.out.println(result.getTimestamp() + ", "+result.getOrdinal());
```

### <a name="update-event"></a> [Update Event](#update-event)

To update an Event to a new version.

```java
// here, the timestamp and ordinal are likely from an earlier
// operation response.
Long timestamp = 1369832019085L;
Long ordinal = 612808568709574700L
LogItem logItem = new LogItem("website", "viewed homepage *corrected to remove sensitive data*");
final EventMetadata updatedMeta =
    client.event("users", "test@test.com")
        .type("eventType")
        .timestamp(timestamp)
        .ordinal(ordinal)
        .update(logItem)
        .get();

// print the 'ref' for the updated event
System.out.println(updatedMeta.getRef());
```

This operation allows for updating an Event by sending in an updated value
 for the Event.


#### <a name="conditional-update-event"></a> [Conditional Update Event](#conditional-update-event)

To update an Event to a new version but only if the ref of the Event being updated matches
the provided value. This insures that the update is being applied to the expected version.

```java
// here, the timestamp and ordinal are likely from an earlier
// operation response.
Long timestamp = 1369832019085L;
Long ordinal = 612808568709574700L
final Event<LogItem> currentEvent =
        client.event("users", "test@test.com")
              .type("activities")
              .timestamp(timestamp)
              .ordinal(ordinal)
              .get(LogItem.class)
              .get();

LogItem logItem = currentEvent.getValue();
logItem.setDescription("Updated!");

final EventMetadata updatedMeta =
    client.event("users", "test@test.com")
        .type("activities")
        .timestamp(currentEvent.getTimestamp())
        .ordinal(currentEvent.getOrdinal())
        .ifMatch(currentEvent.getRef())
        .update(logItem)
        .get();

// print the 'ref' for the updated event
System.out.println(updatedMeta.getRef());
```

This operation allows for updating an Event by sending in an updated value
 for the Event.

### <a name="partial-update-event"></a> [Partial Update Event](#partial-update-event)

To update only a portion of an Event (eg to update a few fields).

```java
// here, the timestamp and ordinal are likely from an earlier
// operation response.
Long timestamp = 1369832019085L;
Long ordinal = 612808568709574700L
final EventMetadata eventMetadata =
    client.event("users", "test@test.com")
        .type("activities")
        .timestamp(timestamp)
        .ordinal(ordinal)
        .patch(JsonPatch.builder()
            .replace("description", "Updated!")
            .build()
        )
        .get();

// print the 'ref' for the updated event
System.out.println(eventMetadata.getRef());
```

This operation allows for updating a portion of an Event by applying a
 list of operations. Each of the operations will be applied to the specified
 event in order. If any of the operations fails for any reason, then the
 patch is aborted, and none of the changes are applied.
 The response metadata will contain the new 'ref' for the updated event. For
 a full list of supported Ops, see the JsonPatch [Javadocs](/javadoc/latest/io/orchestrate/client/jsonpatch/JsonPatch.html).

#### <a name="conditional-partial-update-event"></a> [Conditional Partial Update Event](#conditional-partial-update-event)
This patch operation also supports the ifMatch conditional. When updating
 individual fields, it is recommended that you keep the 'ref' of the event
 you are modifying. Then, when sending the update, provide the original 'ref'
 to insure the updates are being applied to the expected version.

```java
// here, the timestamp, ordinal, and ref are likely from an earlier
// operation response.
Long timestamp = 1369832019085L;
Long ordinal = 612808568709574700L
String ref = "82eafab14dc84ed3";
final EventMetadata eventMetadata =
    client.event("users", "test@test.com")
        .type("activities")
        .timestamp(timestamp)
        .ordinal(ordinal)
        .ifMatch(ref)
        .patch(JsonPatch.builder()
            .replace("description", "Updated!")
            .build()
        )
        .get();

// print the 'ref' for the updated event
System.out.println(eventMetadata.getRef());
```

#### <a name="merge-update-event"></a> [Merge Update Event](#merge-update-event)

To update an Event by merging it with another (via a JsonMergePatch
 https://tools.ietf.org/html/rfc7386).

```java
// here, the timestamp and ordinal are likely from an earlier
// operation response.
Long timestamp = 1369832019085L;
Long ordinal = 612808568709574700L

String changesJson = "{\"description\":\"Updated!!\"}"
final EventMetadata eventMetadata =
    client.event("users", "test@test.com")
        .type("activities")
        .timestamp(timestamp)
        .ordinal(ordinal)
        .merge(changesJson)
        .get();

// print the 'ref' for the updated event
System.out.println(eventMetadata.getRef());
```

This operation allows for updating an event's JSON document by merging it with
another JSON document that contains new values to apply to the original.

The usefulness of this method in the Java client is likely limited to only
command line type utilities that are processing sets of changes
from other sources (otherwise, it is unlikely a normal Java domain object
will be only partially populated). Due to this limited utility, this
method only takes a JSON string. Please provide feedback if your use case
does not fit this assumption.

#### <a name="conditional-merge-update-event"></a> [Conditional Merge Update Event](#conditional-merge-update-event)
This merge operation also supports the ifMatch conditional. When merging
json documents, it is recommended that you keep the 'ref' of the event
you are modifying. Then, when sending the update, provide the original 'ref'
to insure the updates are being applied to the expected version.

```java
// here, the timestamp, ordinal, and ref are likely from an earlier
// operation response.
Long timestamp = 1369832019085L;
Long ordinal = 612808568709574700L
String ref = "82eafab14dc84ed3";

String changesJson = "{\"description\":\"Updated!!\"}"
final EventMetadata eventMetadata =
    client.event("users", "test@test.com")
        .type("activities")
        .timestamp(timestamp)
        .ordinal(ordinal)
        .ifMatch(ref)
        .merge(changesJson)
        .get();

// print the 'ref' for the updated event
System.out.println(eventMetadata.getRef());
```
### <a name="delete-event"></a> [Delete Event](#delete-event)

To delete an individual Event instance

```java
// here, the timestamp and ordinal are likely from an earlier
// operation response.
Long timestamp = 1369832019085L;
Long ordinal = 612808568709574700L

final Boolean purged =
    client.event("users", "test@test.com")
        .type("activities")
        .timestamp(timestamp)
        .ordinal(ordinal)
        .purge()
        .get();
```

#### <a name="conditional-delete-event"></a> [Conditional Delete Event](#conditional-delete-event)

To delete an individual Event instance, but only if the current version is the expected ref.

```java
// here, the timestamp, ordinal, and ref are likely from an earlier
// operation response.
Long timestamp = 1369832019085L;
Long ordinal = 612808568709574700L
String ref = "82eafab14dc84ed3";

try {
    final Boolean purged =
        client.event("users", "test@test.com")
            .type("activities")
            .timestamp(timestamp)
            .ordinal(ordinal)
            .ifMatch(ref)
            .purge()
            .get();
} catch (ItemVersionMismatchException ex) {
   // the ref did not match.
}
```

## <a name="graph"></a> [Graph](#graph)

While building an application it's possible that you'll want to make associations
 between particular objects of the same type or even objects of completely
 different types that share a property of some kind.

It's this sort of data problem that makes the graph features in Orchestrate
 shine, if you're building a socially-aware application you might want to
 add relations between data like "friend" or "follows".

A graph query is the right choice when you have a starting object that you want
 search from to follow relevant relationships and accumulate interesting
 information.

### <a name="fetch-relations"></a> [Fetch Relations](#fetch-relations)

To fetch objects related to the `key` in the `collection` based on a
 relationship or number of `relation`s.

```java
Iterable<KvObject<User>> results =
        client.relation("users", "test@test.com")
              // here we say the related object result should be mapped as a User
              .get(User.class, "friend")
              .get();

for (KvObject<User> result : results) {
	String friendEmail = result.getKey();
	User user = result.getValue();
    // the raw JSON string
    System.out.println(friendEmail + ": " + user.getName());
}
```

Imagine that we'd like to know the `follow`ers of `users` that are `friend`s of
 the user with key `test@test.com`. This kind of query could look like this.

```java
Iterable<KvObject<User>> results =
        client.relation("users", "test@test.com")
              .get(User.class, "friend", "follow")
              .get();

// same as above
```

### <a name="store-relation"></a> [Store Relation](#store-relation)

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

#### <a name="graph-note"></a> [Note](#graph-note)

Relationships in Orchestrate are uni-directional, to define a bi-directional
 relation you must swap the `collection` <-> `toCollection` and `key` <-> `toKey`
 parameters.

We may lift this restriction in a future release of the client.

### <a name="purge-relation"></a> [Purge Relation](#purge-relation)

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

## <a name="async-api"></a> [Asynchronous API](#async-api)

Any Resource method that returns an OrchestrateRequest will initiate an asynchronous
 http request to the Orchestrate service. For example:

```java
OrchestrateRequest<String> request = 
		client.kv("someCollection", "someKey")
			.get(String.class)
```

The get(Class) method will return an OrchestrateRequest that has been initiated
 asynchronously to the Orchestrate service. To handle the result, you will need to either
 register a listener on that request, or block waiting for the response by calling the
 `get` method on the request.

This is what a typical non-blocking call might look like:

```java
String key = "test@test.com";
User user = new User("test1", "Some description");
final OrchestrateRequest<KvMetadata> addUserRequest =
        client.kv(collection(), key)
                .put(user)
                .on(new ResponseAdapter<KvMetadata>() {
                      @Override
                      public void onFailure(final Throwable error) {
                          // handle error condition
                      }

                      @Override
                      public void onSuccess(final KvMetadata userKvMeta) {
                          System.out.println("User Added. Ref="+userKvMeta.getRef());
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
