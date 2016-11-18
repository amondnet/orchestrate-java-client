There are a very large number of libraries and frameworks in the Java ecosystem,
 as well as many different application development patterns used to simplify
 certain types of work.

The examples below demonstrate how to integrate the Java client into a few of
 these patterns and frameworks.

## <a name="listenable-future"></a> [Listenable Future](#listenable-future)

As well as making a blocking API call to Orchestrate to receive the result of
 a request to the service it's also possible to listen (or subscribe) for the
 result in a fully non-blocking way (i.e. "callback style").

```java
final DomainObject obj = new DomainObject();

final Client client = new OrchestrateClient("your api key");
client.kv("someCollection", "someKey")
      .put(obj)
      .on(new ResponseAdapter<KvMetadata>() {
          @Override
          public void onFailure(final Throwable error) {
              // handle error condition
          }

          @Override
          public void onSuccess(final KvMetadata object) {
              // do something with the result
          }
      });
```

## <a name="data-access-object"></a> [Data Access Object (DAO) Pattern](#data-access-object)

A [Data Access Object](http://en.wikipedia.org/wiki/Data_access_object) is an
 object that provides data storage methods without exposing details of the
 database. They typically provide at least the basic persistence operations,
 possibly with additional methods to take advantage of database specific
 functionality.

```java
public class MyObjectDao {

    public MyObject findOne(final String key) { /* some logic */ }

    public void save(final MyObject myObject) { /* some logic */ }

    public void delete(final MyObject myObject) { /* some logic */ }

    // etc ..
}
```

Instead of you creating this code for every data type in your Java application's
 model, the Java client bundles a generic implementation of this pattern. It
 implements the DAO pattern for you while exposing the _asynchronous_
 operation.

```java
import io.orchestrate.client.dao.GenericAsyncDao;

public class MyObjectDao extends GenericAsyncDao<MyObject> {

    private final Client client;

    public MyObjectDao(final Client client) {
        super(client, "myCollection", MyObject.class);
        this.client = client;
    }

}
```

Using the `MyObjectDao` object shown above, writing data to the service becomes
 even easier.

```java
Client client = new OrchestrateClient("your api key");
MyObjectDao dao = new MyObjectDao(client);

// save my object
KvMetadata kvMetadata = dao.save(new MyObject());
```

You can read more about the `GenericAsyncDao` in the
 [javadocs](/javadoc/latest/io/orchestrate/client/dao/GenericAsyncDao.html).

## <a name="dropwizard"></a> [Dropwizard (Managed)](#dropwizard)

[Dropwizard](http://dropwizard.codahale.com/) is a Java framework for developing
 ops-friendly, high-performance, RESTful web services. If you've not come across
 it before, we're a fan of it, it's well worth checking out.

The Dropwizard framework has the concept of a
 "[Managed Object](http://dropwizard.codahale.com/manual/core/#managed-objects)"
 which are objects that control resources that need to be allocated and stopped
 as necessary. This is done by implementing the `Managed` interface.

```java
public class OrchestrateClientManager implements Managed {

    private final Client client;

    public OrchestrateClientManager(final Client client) {
        this.client = client;
    }

    @Override
    public void start() throws Exception {
        // nothing to do here
    }

    @Override
    public void stop() throws Exception {
        client.close();
    }

}
```

Configuring the Java client for Dropwizard is easy.

## <a name="error-handling"></a> [Error Handling](#error-handling)

When you call `get` an OrchestrateRequest, the client will block for the result. You
 may still register listeners that are run when the result is received from the
 service, even if you ultimately call `get` to block waiting for the result.

When you receive a result it will throw an exception if the client could not
 complete the request or the result could not be deserialized from JSON. In all
cases an unchecked exception will be thrown.

This is done primarily because usually the client can not recover from a bad
 operation, it is likely to be a programming error or a networking problem while
 making the HTTP request. As a result, forcing developers to catch a checked
 exception can result in unnecessary code in a `catch()` block.

To listen for an error in the request you can use a `ResponseListener`.

```java
import io.orchestrate.client.ResponseListener;

public class MyObjectResponseListener implements ResponseListener<MyObject> {

    @Override
    public void onFailure(final Throwable error) {
        /* do something */
    }

    @Override
    public void onSuccess(final Boolean object) {
    }

}
```

You can read more about the `ResponseListener` in the
 [javadocs](/javadoc/latest/io/orchestrate/client/ResponseListener.html).
