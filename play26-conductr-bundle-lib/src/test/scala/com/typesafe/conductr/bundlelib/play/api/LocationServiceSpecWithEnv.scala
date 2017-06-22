package com.typesafe.conductr.bundlelib.play.api

import java.net.{ InetSocketAddress, URI => JavaURI }

import com.typesafe.conductr.lib.AkkaUnitTestWithFixture
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{ CacheDirectives, Location, `Cache-Control` }
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import com.typesafe.conductr.bundlelib.scala.{ CacheLike, LocationCache, URI, URL }

import scala.concurrent.Await
import scala.util.{ Failure, Success }

class LocationServiceSpecWithEnv extends AkkaUnitTestWithFixture("LocationServiceSpecWithEnv") {

  def systemFixture(f: this.FixtureParam) = new {
    implicit val system = f.system
    implicit val timeout = f.timeout
    implicit val mat = ActorMaterializer.create(system)
  }

  "The LocationService functionality in the library" should {

    "be able to look up a named service with a leading slash" in { f =>
      val sys = systemFixture(f)
      import sys._

      val serviceUri = URI("http://service_interface:4711/known")
      val app = new GuiceApplicationBuilder()
        .bindings(new BundlelibModule)
        .disable(classOf[ConductRLifecycleModule])
        .build()
      withServerWithKnownService(serviceUri) {
        running(app) {
          val cache = app.injector.instanceOf[CacheLike]
          val locationService = app.injector.instanceOf[LocationService]
          val service = locationService.lookup("/known", URI(""), cache)
          Await.result(service, timeout.duration) shouldBe Some(serviceUri)
        }
      }
    }

    "be able to look up a named service without a leading slash" in { f =>
      val sys = systemFixture(f)
      import sys._

      val serviceUri = URI("http://service_interface:4711/known")
      val app = new GuiceApplicationBuilder()
        .bindings(new BundlelibModule)
        .disable(classOf[ConductRLifecycleModule])
        .build()
      withServerWithKnownService(serviceUri) {
        running(app) {
          val cache = app.injector.instanceOf[CacheLike]
          val locationService = app.injector.instanceOf[LocationService]
          val service = locationService.lookup("known", URI(""), cache)
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

    val url = URL(Env.serviceLocator.get)
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
