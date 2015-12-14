Orchestrate Java Client
=======================

A high performance, asynchronous Java client to query the [Orchestrate.io](http://orchestrate.io/)
 service.

### About

The Orchestrate.io service is a platform for storing and querying data.

Using Orchestrate you can focus on building applications and adding new features
 while we handle safely storing the data, providing a large variety of ways to
 query the data and keeping the service highly available to support your
 applications as they grow.

You can create an account by signing up at the [Dashboard](https://dashboard.orchestrate.io).

### Getting Started

The client library is available on [Maven Central](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22io.orchestrate%22%20AND%20a%3A%22orchestrate-client%22).

#### Using [Gradle](http://www.gradle.org/)

```groovy
dependencies {
    compile group: 'io.orchestrate', name: 'orchestrate-client', version: '0.12.1'
}
```

#### Using Maven

```xml
<dependency>
    <groupId>io.orchestrate</groupId>
    <artifactId>orchestrate-client</artifactId>
    <version>0.12.1</version>
</dependency>
```

### Example

The client API is designed to mirror the Orchestrate REST API. The client library
 is entirely asynchronous and conforms to the [java.util.concurrent.Future](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Future.html)
 specification in the standard library.

#### Creating a Client

You construct a client using the `API key` for your `Application` which can be
 found in the [Dashboard](https://dashboard.orchestrate.io/) (for help,
 [see here](http://java.orchestrate.io/querying/)).

```java
// An API key looks something like:
//   3854bbd7-0a31-43b0-aa94-66236847a717
Client client = OrchestrateClient.builder("your api key").build();
// or (for convenience)
Client client = new OrchestrateClient("your api key");
```

#### Choosing a Data Center

By default, the Orchestrate client uses 'AWS US East' as its host data center. To use another data center, for example 'AWS EU West', you can switch the host when creating the client:

```java
Client client = OrchestrateClient.builder("your api key")
        .host("https://api.aws-eu-west-1.orchestrate.io")
        .build();
```

For more information on Orchestrate's Multi Data Center features check out [the documentation](http://orchestrate.io/docs/multi-data-center).

#### Fetching Key-Value Data

For example, to fetch an object from a `collection` with a given `key` using
 the fully asynchronous API:

```java
client.kv("someCollection", "someKey")
      .get(DomainObject.class)
      .on(new ResponseAdapter<KvObject<DomainObject>>() {
          @Override
          public void onFailure(final Throwable error) {
              // handle errors
          }

          @Override
          public void onSuccess(final KvObject<DomainObject> object) {
              if (object == null) {
                  // we received a 404, no KV object exists
              }

              DomainObject data = object.getValue();
              // do something with the 'data'
          }
      });
```

Or, with the blocking API:

```java
KvObject<DomainObject> object =
        client.kv("someCollection", "someKey")
              .get(DomainObject.class)
              .get();

if (object == null) {
    // we received a 404, no KV object exists
} else {
    DomainObject data = object.getValue();
    // do something with the 'data'
}
```

The client has operations for [Key-Value](http://java.orchestrate.io/querying/#key-value),
 [Search](http://java.orchestrate.io/querying/#search),
 [Graph](http://java.orchestrate.io/querying/#graph)
 and [Event](http://java.orchestrate.io/querying/#events)
 features from the Orchestrate.io service.

#### <a name="user-guide"></a> User Guide

There's a [User Guide](http://java.orchestrate.io/)
 for the `client` with more code examples and details on tuning the library for
 your server environment.

The Javadocs for the codebase are available
 [here](http://java.orchestrate.io/javadoc/latest).

#### Note

The client API is still in _flux_, we're looking for
 [feedback](http://java.orchestrate.io/feedback/)
 from developers and designing what you need to build incredible applications.

### Developer notes

The client uses the [Grizzly framework](https://grizzly.java.net/) for
 processing HTTP requests and [Jackson JSON parser](http://wiki.fasterxml.com/JacksonHome)
 for marshalling data to and from the [Orchestrate.io](http://orchestrate.io/)
 service.

The codebase requires...

* [Gradle](http://gradle.org) build tool at version `1.6+` and the Java compiler at version `1.6.0` or greater
* [Project Lombok](http://projectlombok.org/), so you'll have to install and configure your IDE accordingly

IDE project setup is done with `gradle eclipse` and `gradle idea`, respectively.

#### Building the codebase

A list of all possible build targets can be displayed by Gradle with
 `gradle tasks`.

In a regular write-compile-test cycle use `gradle test`.

It is recommended to run Gradle with the
 [Build Daemon](http://www.gradle.org/docs/nightly/userguide/userguide_single.html#gradle_daemon)
 enabled to improve performance. e.g. `gradle --daemon` once the daemon is
 running it can be stopped with `gradle --stop`.

#### Running Integration Tests

To run integration tests you'll need to create an application in the Orchestrate
 [Dashboard](https://dashboard.orchestrate.io/), then put the API key into the
 `gradle.properties` as the value for the property `orchestrate.apiKey`.

Once you've configured the build system with an API key, it will create the
 `oio-client-integration-tests` collection when you run integration tests with
 `gradle integTest`.

#### Building the documentation

The documentation sources for the [User Manual](#user-guide) is in the `www`
 folder.

The documentation can be built with [Middleman](http://middlemanapp.com/). To
 run the local server for viewing use `middleman server` and to build compiled
 code for deployment use `middleman build`.

### Contribute

All contributions to the documentation and the codebase are very welcome and
 feel free to open issues on the tracker wherever the documentation needs
 improving.

Lastly, pull requests are always welcome! `:)`

#### Thanks

Many thanks to the following developers for contributing to the client:

* Mårten Gustafson (@chids)
* Henric Englund (@hnrc)
* @chkan and Sport Archive (@sportarchive)
