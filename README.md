# Typesafe ConductR Bundle Library

## Introduction

This project provides a number of libraries to facilitate ConductR's status service and its service lookup service. The libraries are intended to be delivered by the Typesafe Reactive Platform (Typesafe RP) and are structured as follows:

* `"com.typesafe.conductr" %  "conductr-bundle-lib"       % "0.4.0"`
* `"com.typesafe.conductr" %% "scala-conductr-bundle-lib" % "0.4.0"`
* `"com.typesafe.conductr" %% "akka-conductr-bundle-lib"  % "0.4.0"`
* play-conductr-bundle-lib - COMING SOON!

## conductr-bundle-lib

This library provides a base level of functionality mainly formed around constructing the requisite payloads for ConductR's RESTful services. The library is pure Java and has no dependencies other than the JDK.

Two services are covered by this library:

* `com.typesafe.conductr.bundlelib.LocationService`
* `com.typesafe.conductr.bundlelib.StatusService`

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

* 2xx - success - any 200 series response is a success meaning that ConductR has successfully acknowledged the startup signal
* xxx - failure - anything else constitutes an error and you should cause the bundle component to exit

You should also prepare for timing out on a request and exit the bundle component if this occurs.

## scala-conductr-bundle-lib

This library provides a reactive API using only Scala and Java types. There are no dependencies other than `conductr-bundle-lib`, Scala and the JDK and it is designed to be used where there is no Akka or Play dependency in your application.

As with `conductr-bundle-lib` there are two services:

* `com.typesafe.conductr.bundlelib.scala.LocationService`
* `com.typesafe.conductr.bundlelib.scala.StatusService`

Please read the section on `conductr-bundle-lib` for an introduction to these services.

### LocationService

The LocationService looks up service names and processes HTTP's `307` "temporary redirect" responses to return the location of the resolved service (or a `404` if one cannot be found). Many HTTP clients allow the following of redirects, particularly when either of the `HEAD` or `GET` methods are used (other methods may be considered insecure by default). Therefore if the service you are locating is an HTTP one then using a regular HTTP client should require no further work. Here is an example of using the [Dispatch](http://dispatch.databinder.net/Dispatch.html) library:

```scala
val svc = LocationService.getLookupUrl("/someservice", "http://127.0.0.1:9000/someservice")
val svcResp = Http.configure(_.setFollowRedirects(true))(url(svc).OK)
```

The above declares an `svc` val which will either be the one that ConductR provides, or one to use for development running on your machine.

When using HTTP clients, consider having the client cache responses. ConductR will return Cache-Control header information informing the client how to cache.

#### Non HTTP service lookups

If the service you require is not HTTP based then you may use the `LocationService.lookup` function. The following code illustrates how a service may be located in place of creating and dispatching your own payload:

```scala
// This will require an implicit ConnectionContext to
// hold a Scala ExecutionContext. There are different
// ConnectionContexts depending on which flavor of the
// library is being used. For the Scala falvor, an
// ExecutionContext is composed. The ExecutionContext
// is needed as "service" is returned as a Future.
import scala.concurrent.ExecutionContext.Implicits.global

implicit val cc = ConnectionContext(global)
val service = LocationService.lookup("/someservice")
```

`service` is typed `Future[Option[(URI, Option[FiniteDuration])]` meaning that an optional response will be returned at some time in the future. Supposing that this lookup is made during the initialisation of your program, the service you're looking for may not exist. However calling the same function later on may yield the service. This is because services can come and go.

The service response constitutes a URI that describes its location along with an optional duration indicating how long the URI may be cached for. A value of `None` indicates that the service should not be cached.

#### Static service lookup

Some bundle components cannot proceed with their initialisation unless the service can be located. We encourage you to re-factor these components so that they look up services at the time when they are required, given that services can come and go. However if you are somehow stuck with this style of code then you may consider the following blocking code as a temporary measure:

```scala
val resultUri = Await.result(
  LocationService.lookup("/someservice").map(LocationService.toUri),
  sometimeout)
val serviceUri = resultUri.getOrElse {
  if (Env.isRunByConductR) System.exit(70)
  new URI("http://127.0.0.1:9000")
}
```

In the above, the program will exit if a service cannot be located at the time the program initializes; unless the program has not been started by ConductR in which case an alternate URI is provided. Instead of blocking you may also consider using an actor:

```scala
class MyService extends Actor {

  private implicit val cc = ConnectionContext(context.dispatcher)

  override def preStart: Unit =
    LocationService.lookup("/someservice").map(LocationService.toUri).pipeTo(self)

  override def receive: Receive =
    initial

  private def initial: Receive = {
    case Some(someService: URI) =>
      // We now have the service

      context.become(service(someService))

    case None =>
      self ! (if (Env.isRunByConductR) PoisonPill else Some(new URI("http://127.0.0.1:9000")))
  }

  private def service(someService: URI): Receive = {
    // Regular actor receive handling goes here given that we have a service URI now.
  }
}
```

This type of actor is used to handle service processing and should only receive service oriented messages once its dependent service URI is known. This is an improvement on the blocking example provided before, as it will not block. However it still has the requirement that `someservice` must be running at the point of initialisation, and that it continues to run. Neither of these requirements may always be satisfied with a distributed system.

### StatusService

The following code illustrates how your bundle component should register its initial health with ConductR. Calling this function is to be done in place of creating and dispatching your own payload:

```scala
StatusService.signalStartedOrExit()
```

In general, the return value of `signalStartedOrExit` is not used and your program proceeds. If ConductR fails to reply, or replies with an error status then this bundle component will exit.

In case you are interested, the function returns a `Future[Option[Unit]]` where a future `Some(())` indicates that ConductR has successfully acknowledged the startup signal. A future of `None` indicates that the bundle has not been started by ConductR.

## akka-conductr-bundle-lib

This library provides a reactive API using [Akka Http](http://akka.io/docs/) and should be used when you are using Akka. The library depends on `scala-conductr-bundle-lib` and can be used for both Java and Scala.

As with `conductr-bundle-lib` there are two services:

* `com.typesafe.conductr.bundlelib.akka.LocationService`
* `com.typesafe.conductr.bundlelib.akka.StatusService`

Please read the section on `conductr-bundle-lib` and then `scala-conductr-bundle-lib` for an introduction to these services. Other than the `import`s for the types, the only difference in terms of API are usage is how a `ConnectionContext` is established. A `ConnectionContext` for Akka requires an `ActorSystem` at a minimum e.g.:

```scala
implicit val cc = ConnectionContext(system)
```

There is also a lower level method where the `HttpExt` and `ActorFlowMaterializer` are passed in:

```scala
// Establish the http extension and flow materializer in
// consideration of your needs.
val httpExt = Http(system)
val actorFlowMaterializer = ActorFlowMaterializer.create(system)

implicit val cc = ConnectionContext(httpExt, actorFlowMaterializer)
```

### Java

The following example illustrates how status is signalled using the Akka Java API:

```java
ConnectionContext cc = ConnectionContext.create(system);
StatusService.getInstance().signalStartedWithContext(cc);
```

Similarly here is a service lookup:

```java
ConnectionContext cc = ConnectionContext.create(system);
LocationService.getInstance().lookupWithContext("/whatever", cc)
```