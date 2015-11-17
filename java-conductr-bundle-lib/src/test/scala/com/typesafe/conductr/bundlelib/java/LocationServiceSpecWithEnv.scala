package com.typesafe.conductr.bundlelib.java

import java.net.{ URI, URL, InetSocketAddress }
import java.util.Optional

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{ CacheDirectives, Location, `Cache-Control` }
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse, StatusCodes, Uri }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import com.typesafe.conductr.AkkaUnitTest
import com.typesafe.conductr.java.Await

import scala.util.{ Failure, Success }

class LocationServiceSpecWithEnv extends AkkaUnitTest("LocationServiceSpecWithEnv", "akka.loglevel = INFO") {

  import scala.concurrent.ExecutionContext.Implicits.global

  "The LocationService functionality in the library" should {
    "return the lookup url" in {
      LocationService.getLookupUrl("/whatever", new URL("http://127.0.0.1/whatever")) shouldBe new URL("http://127.0.0.1:20008/services/whatever")
    }

    "be able to look up a named service" in {
      val serviceUri = new URI("http://service_interface:4711/known")
      withServerWithKnownService(serviceUri) {
        val cache = new LocationCache()
        val service = LocationService.lookup("/known", new URI(""), cache)
        Await.result(service, timeout.duration) shouldBe Optional.of(serviceUri)
      }
    }

    "be able to look up a named service using a cache" in {
      val serviceUri = new URI("http://service_interface:4711/known")
      withServerWithKnownService(serviceUri) {
        val cache = new LocationCache()
        val service = LocationService.lookup("/known", new URI(""), cache)
        Await.result(service, timeout.duration) shouldBe Optional.of(serviceUri)
      }
    }

    "be able to look up a named service and return maxAge" in {
      val serviceUri = new URI("http://service_interface:4711/known")
      withServerWithKnownService(serviceUri, Some(10)) {
        val cache = new LocationCache()
        val service = LocationService.lookup("/known", new URI(""), cache)
        Await.result(service, timeout.duration) shouldBe Optional.of(serviceUri)
      }
    }

    "get back None for an unknown service" in {
      val serviceUrl = new URI("http://service_interface:4711/known")
      withServerWithKnownService(serviceUrl) {
        val cache = new LocationCache()
        val service = LocationService.lookup("/unknown", new URI(""), cache)
        Await.result(service, timeout.duration) shouldBe Optional.empty()
      }
    }
  }

  def withServerWithKnownService(serviceUri: java.net.URI, maxAge: Option[Int] = None)(thunk: => Unit): Unit = {
    implicit val materializer = ActorMaterializer()

    val probe = new TestProbe(system)

    val handler =
      path("services" / Rest) { serviceName =>
        get {
          complete {
            serviceName match {
              case "known" =>
                val uri = Uri(serviceUri.toString)
                val headers = Location(uri) :: (maxAge match {
                  case Some(maxAgeSecs) =>
                    `Cache-Control`(
                      CacheDirectives.`private`(Location.name),
                      CacheDirectives.`max-age`(maxAgeSecs)) :: Nil
                  case None =>
                    Nil
                })
                HttpResponse(StatusCodes.TemporaryRedirect, headers, HttpEntity(s"Located at $uri"))
              case _ =>
                HttpResponse(StatusCodes.NotFound)
            }
          }
        }
      }

    val url = new URL(Env.SERVICE_LOCATOR.get)
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
