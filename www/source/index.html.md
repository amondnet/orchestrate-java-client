A high performance, asynchronous Java client to query the Orchestrate.io service.

The client uses the [Grizzly framework](https://grizzly.java.net/) for
 processing HTTP requests and [Jackson JSON parser](http://wiki.fasterxml.com/JacksonHome)
 for marshalling data to and from the [Orchestrate.io](http://orchestrate.io/)
 service.

## <a name="about"></a> [About Orchestrate](#about)

The [Orchestrate.io](http://orchestrate.io/) service is a platform for storing
 and querying data.

We've designed the platform around the promise of removing the burden of managing
 multiple databases, handling the uptime of those systems and ensuring that
 your applications are still able to read and write data in the event of a
 "flash crowd" (a dramatically increase in traffic that requires the data system
 to scale rapidly to cope with the increase demand on resources).

Using Orchestrate.io you can focus on building applications and adding new
 features while we handle safely storing the data, providing a large variety of
 ways to query the data and keeping the service highly available to support your
 applications as they grow.

### <a name="create-account"></a> [Creating an account](#create-account)

You can create an account by signing up at our
 [Dashboard](https://dashboard.orchestrate.io/).

[https://dashboard.orchestrate.io](https://dashboard.orchestrate.io)

## <a name="getting-started"></a> [Getting Started](#getting-started)

Once you've created an account with the Orchestrate.io Dashboard you're ready to
 build an application with the Java client.

All operations to the Orchestrate.io service happen in the context of an
 `Application` and you can create one from the Dashboard. An `Application` can
 contain any number of `Collection`s. A `Collection` is a namespace for data of
 the same type, in exactly the same way as a table in a `SQL` database.

For example, if you were building a social network, you might have a single
 application named “MyPlace”. The application would then have multiple collections
 within it, based on the different types/groups of information you are storing.
 There might be collections named "users", "pages" and "games".

When first setting up your application, you will need to create it via the
 [Dashboard](https://dashboard.orchestrate.io/). This creation process will
 generate an API key which the Java client uses to access the data in the
 application. Note that while the `Application` must be created manually, it's
 not necessary to create Collections up front — collections are created
 automatically on the first write.

To start reading and writing data to your application, have a look at
 [Querying](/querying.html)

For more information check out the [Getting Started](https://dashboard.orchestrate.io/getting_started)
 section in the Dashboard.

## <a name="managed-dependency"></a> [Managed Dependency](#managed-dependency)

The Orchestrate Java Client is available on
 [Maven Central](http://search.maven.org/#search|gav|1|g%3A%22io.orchestrate%22%20AND%20a%3A%22orchestrate-client%22).

### <a name="gradle-deps"></a> [Gradle](#gradle-deps)

```groovy
dependencies {
    compile(
[group: 'io.orchestrate', name: 'orchestrate-client', version: '0.11.1']
    )
}
```

### <a name="maven-deps"></a> [Maven](#maven-deps)

```xml
<dependency>
    <groupId>io.orchestrate</groupId>
    <artifactId>orchestrate-client</artifactId>
    <version>0.11.1</version>
</dependency>
```

## <a name="download"></a> [Download](#download)

If you're not using Maven (or a dependency resolver that's compatible with Maven
 repositories), you can download the JARs you need for your project from Maven
 Central.

### <a name="java-client-source"></a> [Source Code](#java-client-source)</a>

The codebase for this library is open source on
 [GitHub](https://github.com/orchestrate-io/orchestrate-java-client):

[https://github.com/orchestrate-io/orchestrate-java-client](https://github.com/orchestrate-io/orchestrate-java-client)

Code licensed under the [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0).
 Documentation licensed under [CC BY 3.0](http://creativecommons.org/licenses/by/3.0/).

### <a name="java-client-contribute"></a> [Contribute](#java-client-contribute)

All contributions to the documentation and the codebase are very welcome. Feel
 free to open issues on the tracker wherever the documentation or code needs
 improving.

Also, pull requests are always welcome\! `:)`

#### <a name="java-client-note"></a> [Note](#java-client-note)

The client API is still in _flux_, we're looking for [feedback](/feedback.html)
 from developers and learning what you need to build incredible applications.

## <a name="javadoc"></a> [Javadoc](#javadoc)

The javadoc for the latest version of the client is available at:

[http://java.orchestrate.io/javadoc/latest](http://java.orchestrate.io/javadoc/latest)

For older versions of the documentation:

* [0.12.1](http://java.orchestrate.io/javadoc/0.12.1/)
* [0.12.0](http://java.orchestrate.io/javadoc/0.12.0/)
* [0.11.1](http://java.orchestrate.io/javadoc/0.11.1/)
* [0.11.0](http://java.orchestrate.io/javadoc/0.11.0/)
* [0.10.0](http://java.orchestrate.io/javadoc/0.10.0/)
* [0.9.0](http://java.orchestrate.io/javadoc/0.9.0/)
* [0.8.0](http://java.orchestrate.io/javadoc/0.8.0/)
* [0.7.0](http://java.orchestrate.io/javadoc/0.7.0/)
* [0.6.0](http://java.orchestrate.io/javadoc/0.6.0/)
* [0.5.0](http://java.orchestrate.io/javadoc/0.5.0/)
* [0.4.0](http://java.orchestrate.io/javadoc/0.4.0/)
* [0.3.2](http://java.orchestrate.io/javadoc/0.3.2/)
* [0.3.1](http://java.orchestrate.io/javadoc/0.3.1/)
* [0.3.0](http://java.orchestrate.io/javadoc/0.3.0/)
* [0.2.0](http://java.orchestrate.io/javadoc/0.2.0/)
* [0.1.0](http://java.orchestrate.io/javadoc/0.1.0/)
