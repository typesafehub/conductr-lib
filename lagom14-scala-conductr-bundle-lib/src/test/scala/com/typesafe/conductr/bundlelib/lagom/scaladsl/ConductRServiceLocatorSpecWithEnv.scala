package com.typesafe.conductr.bundlelib.lagom.scaladsl

import java.net.{ InetSocketAddress, URI => JavaURI }

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{ CacheDirectives, Location, `Cache-Control` }
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import com.lightbend.lagom.internal.client.CircuitBreakerMetricsProviderImpl
import com.lightbend.lagom.internal.spi.CircuitBreakerMetricsProvider
import com.lightbend.lagom.scaladsl.api.{ Descriptor, Service, ServiceAcl, ServiceInfo }
import com.lightbend.lagom.scaladsl.client.CircuitBreakerComponents
import com.lightbend.lagom.scaladsl.server.{ LagomApplication, LagomApplicationContext }
import com.typesafe.conductr.bundlelib.play.api.{ Env => PlayEnv }
import com.typesafe.conductr.bundlelib.scala.{ URI, URL }
import com.typesafe.conductr.lib.AkkaUnitTestWithFixture
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
import play.api.test.Helpers._

import scala.collection.immutable
import scala.concurrent.Await
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
