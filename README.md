# Typesafe ConductR Bundle Library

## Introduction

This project provides a number of libraries to facilitate ConductR's status service and its service lookup service. The libraries are intended to be delivered by the Typesafe Reactive Platform (Typesafe RP) and are structured as follows:

* "com.typesafe.conductr" % "conductr-bundle-lib" % "0.1.2"
* "com.typesafe.conductr" %% "scala-conductr-bundle-lib" % "0.1.2"
* akka-conductr-bundle-lib
* play-conductr-bundle-lib

## conductr-bundle-lib

This library provides a base level of functionality mainly formed around constructing the requisite payloads for ConductR's RESTful services. The library is pure Java and has no dependencies other than the JDK.

Two services are covered by this library:

* LocationService
* StatusService

### Location Service

ConductR's location service is able to respond with a URL declaring where a given service (as named by a bundle component's endpoint) resides. The http payload can be constructed as follows:

```java
HttpPayload payload = LocationService.createLookupPayload("/someservice")
```

The `HttpPayload` object may then be queried for elements that will help you make an http request. These methods are:

* `getUrl`
* `getRequestMethod`
* `getFollowRedirects`

When processing the http response you should check for the following http status codes:

* 308 - permanent redirect - the service can be found at the location indicated by the `Location` header
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

The following code illustrates how a service may be located in place of creating and dispatching your own payload:

```scala
// This will require an implicit ExecutionContext
// as "service" is returned as a Future.
val service = LocationService.lookup("/someservice")
```

`service` is typed `Future[Option[URL, Option[FiniteDuration]]` meaning that an optional response will be returned at some time in the future. Supposing that this lookup is made during the initialisation of your program, the service you're looking for may not exist. However calling the same function later on may yield the service. This is because services can come and go.

The service response constitutes a URL that describes its location along with an optional duration indicating how long the URL may be cached for. A value of `None` indicates that the service should not be cached.

### StatusService

The following code illustrates how your bundle component should register its initial health with ConductR. Calling this function is to be done in place of creating and dispatching your own payload:

```scala
// This will require an implicit ExecutionContext
// as the signalling is performed asynchronously.
StatusService.signalStarted()
```

In general, the return value of `signalStartedOrExit` is not used and your program proceeds. If ConductR fails to reply, or replies with an error status then this bundle component will exit.

In case you are interested, the function returns a `Future[Option[Unit]]` where a future `Some(())` indicates that ConductR has successfully acknowledged the startup signal. A future of `None` indicates that the bundle has not been started by ConductR.
