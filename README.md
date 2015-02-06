# Typesafe ConductR Bundle Library

## Introduction

This project provides a number of libraries to facilitate ConductR's status service and its service lookup service. The libraries are intended to be delivered by the Typesafe Reactive Platform (Typesafe RP) and are structured as follows:

* "com.typesafe.conductr" % "conductr-bundle-lib" % "0.1.5"
* "com.typesafe.conductr" %% "scala-conductr-bundle-lib" % "0.1.5"
* akka-conductr-bundle-lib
* play-conductr-bundle-lib

## conductr-bundle-lib

This library provides a base level of functionality mainly formed around constructing the requisite payloads for ConductR's RESTful services. The library is pure Java and has no dependencies other than the JDK.

Two services are covered by this library:

* LocationService
* StatusService

### Location Service

ConductR's location service is able to respond with a URI declaring where a given service (as named by a bundle component's endpoint) resides. The http payload can be constructed as follows:

```java
HttpPayload payload = LocationService.createLookupPayload("/someservice")
```

The `HttpPayload` object may then be queried for elements that will help you make an http request. These methods are:

* `getUrl`
* `getRequestMethod`
* `getFollowRedirects`

When processing the http response you should check for the following http status codes:

* 307 - temporary redirect - the service can be found at the location indicated by the `Location` header. `Cache-Control` may also be supplied to indicate how long the location may be cached for.
* 404 - not found - the service cannot be located at this time

You should also prepare for timing out on a request and process as per a 404.

### Status Service

Conduct's status service is required by a bundle component in order to signal when it has started. A successful startup is anything that the application is required to do to become available for processing. For example, this may involved validating configuration. The http payload can be constructed as follows:

 ```java
 HttpPayload payload = StatusService.createSignalStartedPayload()
 ```

The `HttpPayload` object may then be queried for elements in the same way as for the location service of the previous section.

When processing the http response you should check for the following http status codes:

* 2xx - success - any 200 series response is a success meaning that ConductR has
* xxx - failure - anything else constitutes an error and you should cause the bundle component to exit

You should also prepare for timing out on a request and exit the bundle component if this occurs.

## scala-conductr-bundle-lib

This library provides a reactive API using only Scala and Java types. There are no dependencies other than `conductr-bundle-lib`, Scala and the JDK and it is designed to be used where there is no Akka or Play dependency in your application.

As with `conductr-bundle-lib` there are two services:

* LocationService
* StatusService

Please read the section on `conductr-bundle-lib` for an introduction to these services.

### LocationService

The LocationService looks up service names and processes HTTP's `307` "temporary redirect" responses to return the location of the resolved service (or a `404` if one cannot be found). Many HTTP clients allow the following of redirects, particularly when either of the `HEAD` or `GET` methods are used (other methods may be considered insecure by default). Therefore if the service you are locating is an HTTP one then using a regular HTTP client should require no further work. Here is an example of using the [Dispatch](http://dispatch.databinder.net/Dispatch.html) library:

```scala
val serviceLocator = Option(Env.SERVICE_LOCATOR).getOrElse("http://127.0.0.1:9000")

val svc = Option(Env.SERVICE_LOCATOR)
  .map(serviceLocator => s"serviceLocator/someservice")
  .getOrElse("http://127.0.0.1:9000/someservice")
val svcResp = Http.configure(_.setFollowRedirects(true))(url(svc).OK)
```

The above declares a `serviceLocator` val which will either be the one that ConductR provides, or one to use for development that runs on your machine.

When using HTTP clients, consider having the client cache responses. ConductR will return Cache-Control header information informing the client how to cache.

#### Non HTTP service lookups

If the service you require is not HTTP based then you may use the `LocationService.lookup` function. The following code illustrates how a service may be located in place of creating and dispatching your own payload:

```scala
// This will require an implicit ExecutionContext
// as "service" is returned as a Future.
val service = LocationService.lookup("/someservice")
```

`service` is typed `Future[Option[URI, Option[FiniteDuration]]` meaning that an optional response will be returned at some time in the future. Supposing that this lookup is made during the initialisation of your program, the service you're looking for may not exist. However calling the same function later on may yield the service. This is because services can come and go.

The service response constitutes a URI that describes its location along with an optional duration indicating how long the URI may be cached for. A value of `None` indicates that the service should not be cached.

#### Static service lookup

Some bundle components cannot proceed with their initialisation unless the service can be located. We encourage you to re-factor these components so that they look up services at the time when they are required, given that services can come and go. However if you are somehow stuck with this style of code then we offer a utility that causes an exit if the lookup results in no service being found:

```scala
val default = new URI("http://127.0.0.1:9000")
val url = LocationService.lookup("/someservice").map(LocationService.getUriOrExit(default))
```

Note that the above returns a `Future[URI]` and it will exit in the case of ConductR running and the lookup failing. If ConductR is not running and the lookup fails then the `default` value will be returned.

### StatusService

The following code illustrates how your bundle component should register its initial health with ConductR. Calling this function is to be done in place of creating and dispatching your own payload:

```scala
// This will require an implicit ExecutionContext
// as the signalling is performed asynchronously.
StatusService.signalStartedOrExit()
```

In general, the return value of `signalStartedOrExit` is not used and your program proceeds. If ConductR fails to reply, or replies with an error status then this bundle component will exit.

In case you are interested, the function returns a `Future[Option[Unit]]` where a future `Some(())` indicates that ConductR has successfully acknowledged the startup signal. A future of `None` indicates that the bundle has not been started by ConductR.
