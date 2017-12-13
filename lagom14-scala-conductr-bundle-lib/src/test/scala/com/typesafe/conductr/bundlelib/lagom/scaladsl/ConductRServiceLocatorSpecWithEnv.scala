package com.typesafe.conductr.bundlelib.lagom.scaladsl

import java.net.{ InetSocketAddress, URI => JavaURI }

import akka.NotUsed
import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{ CacheDirectives, Location, `Cache-Control` }
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import com.lightbend.lagom.internal.client.CircuitBreakerMetricsProviderImpl
import com.lightbend.lagom.internal.spi.CircuitBreakerMetricsProvider
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceAcl, ServiceCall, ServiceInfo }
import com.lightbend.lagom.scaladsl.client.CircuitBreakerComponents
import com.lightbend.lagom.scaladsl.server.{ LagomApplication, LagomApplicationContext, LagomApplicationLoader }
import com.typesafe.conductr.bundlelib.play.api.{ Env => PlayEnv }
import com.typesafe.conductr.bundlelib.scala.{ URI, URL }
import com.typesafe.conductr.lib.AkkaUnitTestWithFixture
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
import play.api.test.Helpers._

import scala.collection.immutable
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }

object ConductRServiceLocatorSpecWithEnv {
  class DummyService extends Service {
    override def descriptor: Descriptor = {
      import Service._
      named("dummy")
    }
  }
}

class ConductRServiceLocatorSpecWithEnv extends AkkaUnitTestWithFixture("ConductRServiceLocatorSpecWithEnv") {

  def systemFixture(f: this.FixtureParam) = new {
    implicit val system = f.system
    implicit val timeout = f.timeout
    implicit val mat = ActorMaterializer.create(system)
    val app = new LagomApplication(LagomApplicationContext.Test) with AhcWSComponents with ConductRServiceLocatorComponents with CircuitBreakerComponents {
      override lazy val lagomServer = serverFor[ConductRServiceLocatorSpecWithEnv.DummyService](new ConductRServiceLocatorSpecWithEnv.DummyService())
      override lazy val actorSystem = system
      override lazy val materializer = mat
      override lazy val executionContext = actorSystem.dispatcher
      override lazy val router = Router.empty
      override lazy val circuitBreakerMetricsProvider: CircuitBreakerMetricsProvider = new CircuitBreakerMetricsProviderImpl(actorSystem)
      override lazy val serviceInfo: ServiceInfo = ServiceInfo("conductr-service.-est", Map.empty[String, immutable.Seq[ServiceAcl]])
    }
  }

  "The ConductR service locator" should {

    "be able to look up a named service with a leading slash" in { f =>
      val sys = systemFixture(f)
      import sys._

      val serviceUri = URI("http://service_interface:4711/known")

      withServerWithKnownService(serviceUri) {
        running(app.application) {
          val service = app.serviceLocator.locate("/known", Descriptor.NoCall)
          Await.result(service, timeout.duration) shouldBe Some(serviceUri)
        }
      }
    }

    "be able to look up a named service without a leading slash" in { f =>
      val sys = systemFixture(f)
      import sys._

      val serviceUri = URI("http://service_interface:4711/known")
      withServerWithKnownService(serviceUri) {
        running(app.application) {
          val service = app.serviceLocator.locate("known", Descriptor.NoCall)
          Await.result(service, timeout.duration) shouldBe Some(serviceUri)
        }
      }
    }

    "be able to be mixed in into a lagom application" in { _ =>
      // Don't be surprised if you see all these Lagom definitions declared here that does nothing.
      // This is intentional.
      // We have these declarations to assert that the application will compile successfully by mixing in
      // ConductRApplicationComponents.
      // This test is put in place as there were changes in the Lagom 1.4 API that will break user's code compilation
      // due to ConductRApplicationComponents not extending CircuitBreakersComponent.
      // Reference: https://github.com/typesafehub/conductr-lib/pull/162#issuecomment-351240570

      trait HelloService extends Service {
        def hello(input: String): ServiceCall[NotUsed, String]
        override final def descriptor: Descriptor = {
          import Service._
          named("hello")
            .withCalls(restCall(Method.GET, "/hello/:text", hello _))
            .withAutoAcl(true)
        }
      }

      class HelloServiceImpl extends HelloService {
        override def hello(input: String): ServiceCall[NotUsed, String] = ServiceCall(_ => Future.successful(input.toUpperCase))
      }

      abstract class HelloApplication(context: LagomApplicationContext) extends LagomApplication(context) with AhcWSComponents {
        override def lagomServer = serverFor[HelloService](new HelloServiceImpl)
      }

      class HelloApplicationLoader extends LagomApplicationLoader {
        override def load(context: LagomApplicationContext) =
          new HelloApplication(context) with ConductRApplicationComponents
        override def loadDevMode(context: LagomApplicationContext) =
          new HelloApplication(context) with ConductRApplicationComponents
        override def describeService = Some(readDescriptor[HelloService])
      }

    }
  }

  def withServerWithKnownService(serviceUri: JavaURI, maxAge: Option[Int] = None)(thunk: => Unit)(implicit system: ActorSystem, mat: ActorMaterializer): Unit = {
    import system.dispatcher

    val probe = new TestProbe(system)

    val handler =
      path("services" / Remaining) { serviceName =>
        get {
          complete {
            serviceName match {
              case "known" =>
                val headers = Location(serviceUri.toString) :: (maxAge match {
                  case Some(maxAgeSecs) =>
                    `Cache-Control`(
                      CacheDirectives.`private`(Location.name),
                      CacheDirectives.`max-age`(maxAgeSecs)
                    ) :: Nil
                  case None =>
                    Nil
                })
                HttpResponse(StatusCodes.TemporaryRedirect, headers, HttpEntity(s"Located at $serviceUri"))
              case _ =>
                HttpResponse(StatusCodes.NotFound)
            }
          }
        }
      }

    val url = URL(PlayEnv.serviceLocator.get)
    val server = Http(system).bindAndHandle(handler, url.getHost, url.getPort)

    try {
      server.onComplete {
        case Success(binding) => probe.ref ! binding.localAddress
        case Failure(e)       => probe.ref ! e
      }

      val address = probe.expectMsgType[InetSocketAddress]
      address.getHostString should be(url.getHost)
      address.getPort should be(url.getPort)

      thunk
    } finally {
      server.foreach(_.unbind())
    }
  }
}
