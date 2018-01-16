# Typesafe ConductR Bundle Library

[![Build Status](https://api.travis-ci.org/typesafehub/conductr-lib.png?branch=master)](https://travis-ci.org/typesafehub/conductr-lib)

## Introduction

This project provides a number of libraries to facilitate [ConductR](http://typesafe.com/products/conductr)'s status service and its service lookup service. Note that usage of the libraries in your code is entirely benign when used outside of the context of ConductR i.e. you will find that your applications and services will continue to function normally when used without ConductR. We have also designed the libraries to be a convenience to ConductR's REST and environment variable based APIs, and to have a very low impact on your code.

Add one of the following libraries to your project.

* `"com.typesafe.conductr" %  "java-conductr-bundle-lib"    % "2.1.1"`
* `"com.typesafe.conductr" %% "scala-conductr-bundle-lib"   % "2.1.1"`
* `"com.typesafe.conductr" %% "akka24-conductr-bundle-lib"  % "2.1.1"`
* `"com.typesafe.conductr" %% "akka25-conductr-bundle-lib"  % "2.1.1"`
* `"com.typesafe.conductr" %% "play25-conductr-bundle-lib"  % "2.1.1"`
* `"com.typesafe.conductr" %% "play26-conductr-bundle-lib"  % "2.1.1"`
* `"com.typesafe.conductr" %% "lagom13-java-conductr-bundle-lib"  % "2.1.1"`
* `"com.typesafe.conductr" %% "lagom13-scala-conductr-bundle-lib" % "2.1.1"`
* `"com.typesafe.conductr" %% "lagom14-java-conductr-bundle-lib"  % "2.1.1"`
* `"com.typesafe.conductr" %% "lagom14-scala-conductr-bundle-lib" % "2.1.1"`

Note that the examples here use the following import to conveniently build the JDK `URI` and `URL` types.

```scala
import com.typesafe.conductr.bundlelib.scala.{URL, URI}
```

## Table of contents

* [conductr-bundle-lib](#conductr-bundle-lib)
* [scala-conductr-bundle-lib](#scala-conductr-bundle-lib)
* [akka[24|25]-conductr-bundle-lib](#akka2425-conductr-bundle-lib)
* [play[25|26]-conductr-bundle-lib](#play2526-conductr-bundle-lib)
* [lagom[13|14]-java-conductr-bundle-lib](#lagom1314-java-conductr-bundle-lib)
* [lagom[13|14]-scala-conductr-bundle-lib](#lagom1314-scala-conductr-bundle-lib)

## conductr-bundle-lib

This library provides a base level of functionality mainly formed around constructing the requisite payloads for ConductR's RESTful services. The library is pure Java and has no dependencies other than the JDK.

Two services are covered by this library:

* `com.typesafe.conductr.bundlelib.LocationService`
* `com.typesafe.conductr.bundlelib.StatusService`

### Location Service

ConductR's location service is able to respond with a URI declaring where a given service (as named by a bundle component's endpoint) resides. The http payload can be constructed as follows:

```java
HttpPayload payload = LocationService.createLookupPayload("someservice")
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
val svc = LocationService.getLookupUrl("someservice", URL("http://127.0.0.1:9000/someservice"))
val svcResp = Http.configure(_.setFollowRedirects(true))(url(svc.toString).OK)
```

The above declares an `svc` val which will either be the one that ConductR provides, or one to use for development running on your machine.

When using HTTP clients, consider having the client cache responses. ConductR will return Cache-Control header information informing the client how to cache.

#### Non HTTP service lookups

If the service you require is not HTTP based then you may use the `LocationService.lookup` function. The following code illustrates how a service may be located in place of creating and dispatching your own payload. The sample also shows how to use a cache provided specifically for these lookups (note use com.typesafe.lib.scala for 1.2 of this library onwards):

```scala
// This will require an implicit ConnectionContext to
// hold a Scala ExecutionContext. There are different
// ConnectionContexts depending on which flavor of the
// library is being used. For the Scala flavor, a Scala
// ExecutionContext is composed. The ExecutionContext
// is needed as "service" is returned as a Future.
// For convenience, we provide a global ConnectionContext
// that may be imported.
import com.typesafe.conductr.bundlelib.scala.ConnectionContext.Implicits.global

val locationCache = LocationCache()

val service = LocationService.lookup("someservice", URI("tcp://localhost:1234"), locationCache)
```

`service` is typed `Future[Option[URI]]` meaning that an optional URI response will be returned at some time in the future. Supposing that this lookup is made during the initialisation of your program, the service you're looking for may not exist. However calling the same function later on may yield the service. This is because services can come and go. Note that the fallback URI of `"tcp://localhost:1234"` will be returned if this function is called upon when started outside of ConductR.

The service response constitutes a URI that describes its location.

#### Static service lookup

Some bundle components cannot proceed with their initialisation unless the service can be located. We encourage you to re-factor these components so that they look up services at the time when they are required, given that services can come and go. However if you are somehow stuck with this style of code then you may consider the following blocking code as a temporary measure:

```scala
val resultUri = Await.result(
  LocationService.lookup("someservice", URI("http://127.0.0.1:9000"), locationCache),
  sometimeout)
val serviceUri = resultUri.getOrElse(System.exit(70))
```

In the above, the program will exit if a service cannot be located at the time the program initializes; unless the program has not been started by ConductR in which case an alternate URI is provided.

### StatusService

The following code illustrates how your bundle component should register its initial health with ConductR. Calling this function is to be done in place of creating and dispatching your own payload:

```scala
StatusService.signalStartedOrExit()
```

In general, the return value of `signalStartedOrExit` is not used and your program proceeds. If ConductR fails to reply, or replies with an error status then this bundle component will exit.

In case you are interested, the function returns a `Future[Option[Unit]]` where a future `Some(())` indicates that ConductR has successfully acknowledged the startup signal. A future of `None` indicates that the bundle has not been started by ConductR.

## akka[24|25]-conductr-bundle-lib

This library provides a reactive API using [Akka Http](http://akka.io/docs/) and should be used when you are using Akka. The library depends on `scala-conductr-bundle-lib` and can be used for both Java and Scala.

As with `conductr-bundle-lib` there are these two services:

* `com.typesafe.conductr.bundlelib.akka.LocationService`
* `com.typesafe.conductr.bundlelib.akka.StatusService`

and there is also another:

* `com.typesafe.conductr.bundlelib.akka.Env`

Please read the section on `conductr-bundle-lib` and then `scala-conductr-bundle-lib` for an introduction to these services. The `Env` one is discussed in the "Akka Clustering" section below.

Other than the `import`s for the types, the only difference in terms of API are usage is how a `ConnectionContext` is established. A `ConnectionContext` for Akka requires an implicit `ActorSystem` or `ActorContext` at a minimum e.g.:

```scala
 implicit val cc = ConnectionContext()
```

There is also a lower level method where the `HttpExt` and `ActorMaterializer` are passed in:

```scala
implicit val cc = ConnectionContext(httpExt, actorMaterializer)
```

When in the context of an actor, a convenient `ImplicitConnectionContext` trait may be mixed in to establish the `ConnectionContext`. The next section illustrates this in its sample `MyService` actor.

### Static Service Lookup

As a reminder, some bundle components cannot proceed with their initialisation unless the service can be located. We encourage you to re-factor these components so that they look up services at the time when they are required, given that services can come and go. That said, here is a non-blocking improvement on the example provided for the `scala-conductr-bundle-lib`:


```scala
class MyService(cache: CacheLike) extends Actor with ImplicitConnectionContext {

  import context.dispatcher

  override def preStart(): Unit =
    LocationService.lookup("someservice", URI("http://127.0.0.1:9000"), cache).pipeTo(self)

  override def receive: Receive =
    initial

  private def initial: Receive = {
    case Some(someService: URI) =>
      // We now have the service

      context.become(service(someService))

    case None =>
      self ! PoisonPill
  }

  private def service(someService: URI): Receive = {
    // Regular actor receive handling goes here given that we have a service URI now.
    ...
  }
}
```

This type of actor is used to handle service processing and should only receive service oriented messages once its dependent service URI is known. This is an improvement on the blocking example provided before, as it will not block. However it still has the requirement that `someservice` must be running at the point of initialization, and that it continues to run. Neither of these requirements may always be satisfied with a distributed system.

### Java

The following example illustrates how status is signalled using the Akka Java API:

```java
ConnectionContext cc = ConnectionContext.create(system);
StatusService.getInstance().signalStartedOrExitWithContext(cc);
```

Similarly here is a service lookup:

```java
ConnectionContext cc = ConnectionContext.create(system);
LocationService.getInstance().lookupWithContext("whatever", URI("tcp://localhost:1234"), cache, cc)
```

### Akka Clustering

[Akka cluster](http://doc.akka.io/docs/akka/snapshot/scala/cluster-usage.html) based applications or services have a requirement where the first node in a cluster must form the cluster, and the subsequent nodes join with any of the ones that come before them (seed nodes). Where bundles share the same `system` property in their `bundle.conf`, and have an intersection of endpoint names, then ConductR will ensure that only one bundle is started at a time. Thus the first bundle can determine whether it is the first bundle, and subsequent bundles can determine the IP and port numbers of the bundles that have started before them.

In order for an application or service to take advantage of this guarantee provided by ConductR, the following call is required to obtain configuration that will be used when establishing your actor system:

```scala
import com.typesafe.conductr.bundlelib.akka.Env
import com.typesafe.conductr.lib.akka.ConnectionContext

...

val systemName = Env.mkSystemName("MyApp1")
val config = Env.asConfig(systemName)
implicit val system = ActorSystem(systemName, config.withFallback(ConfigFactory.load()))
```

Clusters will then be formed correctly. The above call looks for an endpoint named `akka-remote` by default. Therefore if you must declare the Akka remoting port as seed. The following endpoint declaration within a `build.sbt` shows how:

```scala
BundleKeys.endpoints := Map("akka-remote" -> Endpoint("tcp"))
```

In the above, no declaration of `services` is required as akka remoting is an internal, cluster-wide TCP service.

## play[25|26]-conductr-bundle-lib

> If you are using Play 2.5 or 2.6 then this section is for you.

[sbt-conductr](https://github.com/typesafehub/sbt-conductr) is automatically adding this library to your Play project.

This library provides a reactive API using [Play WS](https://www.playframework.com/documentation/2.5.x/ScalaWS) and should be used when you are using Play. The library depends on `akka24-conductr-bundle-lib` and can be used for both Java and Scala. As per Play's conventions, `play.api` is used for the Scala API and just `play` is used for Java.

As with `conductr-bundle-lib` there are two services:

* `com.typesafe.conductr.bundlelib.play.LocationService` (Java) or `com.typesafe.conductr.bundlelib.play.api.LocationService` (Scala)
* `com.typesafe.conductr.bundlelib.play.StatusService` (Java) or `com.typesafe.conductr.bundlelib.play.api.StatusService` (Scala)

and there is also another:

* `com.typesafe.conductr.bundlelib.play.Env` (Java) or `com.typesafe.conductr.bundlelib.play.api.Env` (Scala)

Please read the section on `conductr-bundle-lib` and then `scala-conductr-bundle-lib` for an introduction to these services. The `Env` one is discussed in the section below. The major difference between the APIs for Play 2.5 and the other variants is that components are expected to be injected. For example, to use the `LocationService` in your controller (Scala):

```scala
class MyGreatController @Inject() (locationService: LocationService, locationCache: CacheLike) extends Controller {
  ...
  locationService.lookup("known", URI(""), locationCache)
  ...
}
```

The following components are available for injection:

* CacheLike
* ConnectionContext
* LocationService
* StatusService

Note that if you are using your own application loader then you should ensure that the Akka and Play ConductR-related properties are loaded. Here's a complete implementation (for Scala):

```scala
class MyCustomApplicationLoader extends ApplicationLoader {
  def load(context: ApplicationLoader.Context): Application = {
    val systemName = AkkaEnv.mkSystemName("application")
    val conductRConfig = Configuration(AkkaEnv.asConfig(systemName)) ++ Configuration(PlayEnv.asConfig(systemName))
    val newConfig = context.initialConfiguration ++ conductRConfig
    val newContext = context.copy(initialConfiguration = newConfig)
    val prodEnv = Environment.simple(mode = Mode.Prod)
    new GuiceApplicationLoader(GuiceApplicationBuilder(environment = prodEnv)).load(newContext)
  }
}
```

## lagom[13|14]-java-conductr-bundle-lib

> If you are using Lagom 1.x with Java, then this section is for you.

[sbt-conductr](https://github.com/typesafehub/sbt-conductr) automatically adds this library to your Lagom project. You don't need set any additional settings for your Lagom services.

Maven users should add the dependency directly to your Lagom service implementation projects. For example:

```xml
<dependency>
    <groupId>com.typesafe.conductr</groupId>
    <artifactId>lagom14-java-conductr-bundle-lib_2.11</artifactId>
    <version>2.1.1</version>
</dependency>
```

Note that if you are using your own application loader then you should ensure that the Akka, Play and Lagom ConductR-related properties are loaded. Here's a complete implementation (in Scala):

```scala
import com.typesafe.conductr.bundlelib.akka.{ Env => AkkaEnv }
import com.typesafe.conductr.bundlelib.play.api.{ Env => PlayEnv }
import play.api._
import play.api.inject.guice.{ GuiceApplicationBuilder, GuiceApplicationLoader }

class MyCustomApplicationLoader extends ApplicationLoader {
  def load(context: ApplicationLoader.Context): Application = {
    val systemName = AkkaEnv.mkSystemName("application")
    val conductRConfig = Configuration(AkkaEnv.asConfig(systemName)) ++ Configuration(PlayEnv.asConfig(systemName))
    val newConfig = context.initialConfiguration ++ conductRConfig
    val newContext = context.copy(initialConfiguration = newConfig)
    val prodEnv = Environment.simple(mode = Mode.Prod)
    new GuiceApplicationLoader(GuiceApplicationBuilder(environment = prodEnv)).load(newContext)
  }
}
```

## lagom[13|14]-scala-conductr-bundle-lib

> If you are using Lagom 1.x with Scala, then this section is for you.

[sbt-conductr](https://github.com/typesafehub/sbt-conductr) automatically adds this library to your Lagom project. This provides components, including the service locator and other components necessary for initialization on ConductR, which you can mix in with your application cake. To do so, mix `ConductRApplicationComponents` into your production cake:

```scala
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._
import com.typesafe.conductr.bundlelib.lagom.scaladsl.ConductRApplicationComponents

class HelloApplicationLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext) =
    new HelloApplication(context) with ConductRApplicationComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new HelloApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[HelloService])
}
```

Once you have added this to each of your services, you should be ready to run in ConductR. Also note that it’s very important to implement the `describeService` method on `LagomApplicationLoader`, as this will ensure that the ConductR sbt tooling is able to correctly discover the Lagom service APIs offered by each service. If using a version of Lagom earlier than 1.3.6, you should implement `describeServices` (which returns `immutable.Seq[Descriptor]`) instead of `describeService`, however returning more than one service descriptor from `describeServices` is not supported by ConductR.

# For Developers

## Releasing

You'll need permissions to release to the typesafe.com organization at Sonatype. You will also require a PGP key.

This projects uses `sbt-release`. To release use the `release` command (no `+` required as a prefix). This will cross publish releases for Scala. Note that you will see messages like this:

```
[trace] Stack trace suppressed: run last conductRBundleLib/*:publish for the full output.
[trace] Stack trace suppressed: run last common/*:publish for the full output.
[error] (conductRBundleLib/*:publish) java.io.IOException: destination file exists and overwrite == false
[error] (common/*:publish) java.io.IOException: destination file exists and overwrite == false
```

This is because the same Java libraries are used for multiple Scala versions. Do not be concerned, the publishing tool is actually just warning you i.e. they are not errors.

Everything is released to Maven Central via the Sonatype repository, including automated staging to release.
