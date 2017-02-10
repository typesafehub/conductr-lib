package com.typesafe.conductr.bundlelib.lagom.javadsl

import java.net.{ InetSocketAddress, URI => JavaURI }

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{ CacheDirectives, Location, `Cache-Control` }
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import com.lightbend.lagom.javadsl.api.Descriptor
import com.typesafe.conductr.bundlelib.play.api.{ ConductRLifecycleModule, Env => PlayEnv }
import com.typesafe.conductr.bundlelib.scala.{ URI, URL }
import com.typesafe.conductr.lib.AkkaUnitTestWithFixture
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._

import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._
import scala.concurrent.Await
import scala.util.{ Failure, Success }

class ConductRServiceLocatorSpecWithEnv extends AkkaUnitTestWithFixture("ConductRServiceLocatorSpecWithEnv") {

  def systemFixture(f: this.FixtureParam) = new {
    implicit val system = f.system
    implicit val timeout = f.timeout
    implicit val mat = ActorMaterializer.create(system)
    implicit val ec = play.api.libs.concurrent.Execution.defaultContext
  }

  "The ConductR service locator" should {

    "be able to look up a named service with a leading slash" in { f =>
      val sys = systemFixture(f)
      import sys._

      val serviceUri = URI("http://service_interface:4711/known")
      // GuiceApplicationBuilder uses the enabled modules from the `reference.conf`
      val app = new GuiceApplicationBuilder()
        .disable(classOf[ConductRLifecycleModule])
        .build()
      withServerWithKnownService(serviceUri) {
        running(app) {
          val serviceLocator = app.injector.instanceOf[ConductRServiceLocator]
          val service = serviceLocator.locate("/known", Descriptor.Call.NONE).toScala.map(_.asScala)
          Await.result(service, timeout.duration) shouldBe Some(serviceUri)
        }
      }
    }

    "be able to look up a named service without a leading slash" in { f =>
      val sys = systemFixture(f)
      import sys._

      val serviceUri = URI("http://service_interface:4711/known")
      // GuiceApplicationBuilder uses the enabled modules from the `reference.conf`
      val app = new GuiceApplicationBuilder()
        .disable(classOf[ConductRLifecycleModule])
        .build()
      withServerWithKnownService(serviceUri) {
        running(app) {
          val serviceLocator = app.injector.instanceOf[ConductRServiceLocator]
          val service = serviceLocator.locate("known", Descriptor.Call.NONE).toScala.map(_.asScala)
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
