package com.typesafe.conductr.bundlelib.akka

import java.net.{ URI => JavaURI, InetSocketAddress }

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{ CacheDirectives, Location, `Cache-Control` }
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import com.typesafe.conductr.bundlelib.scala.{ URL, URI, CacheLike, LocationCache }
import com.typesafe.conductr.lib.{ AkkaUnitTestWithFixture }
import com.typesafe.conductr.lib.akka._
import scala.concurrent.Await
import scala.util.{ Failure, Success }

class LocationServiceSpecWithEnv extends AkkaUnitTestWithFixture("LocationServiceSpecWithEnv") {

  def systemFixture(f: this.FixtureParam) = new {
    implicit val system = f.system
    implicit val cc = ConnectionContext()
    implicit val mat = cc.actorMaterializer
    implicit val timeout = f.timeout
  }

  "The LocationService functionality in the library" should {

    "be able to look up a named service with a leading slash" in { f =>
      val sys = systemFixture(f)
      import sys._

      val serviceUri = URI("http://service_interface:4711/known")
      withServerWithKnownService(serviceUri) {
        val cache = LocationCache()
        val service = LocationService.lookup("/known", URI(""), cache)
        Await.result(service, timeout.duration) shouldBe Some(serviceUri)
      }
    }

    "be able to look up a named service without a leading slash" in { f =>
      val sys = systemFixture(f)
      import sys._

      val serviceUri = URI("http://service_interface:4711/known")
      withServerWithKnownService(serviceUri) {
        val cache = LocationCache()
        val service = LocationService.lookup("known", URI(""), cache)
        Await.result(service, timeout.duration) shouldBe Some(serviceUri)
      }
    }

    "be able to look up a named service within an actor" in { f =>
      val sys = systemFixture(f)
      import sys._

      import akka.pattern.pipe

      class MyService(observer: ActorRef, cache: CacheLike) extends Actor with ImplicitConnectionContext {

        import context.dispatcher

        override def preStart(): Unit =
          LocationService.lookup("/known", URI("http://127.0.0.1:9000"), cache).pipeTo(self)

        override def receive: Receive = {
          case Some(someService: JavaURI) =>
            // We now have the service

            observer ! someService

          case None =>
            self ! PoisonPill
        }
      }

      val serviceUri = URI("http://service_interface:4711/known")
      withServerWithKnownService(serviceUri) {

        val testProbe = TestProbe()
        val cache = LocationCache()

        system.actorOf(Props(new MyService(testProbe.ref, cache)))
        testProbe.expectMsg(serviceUri)
      }
    }
  }

  def withServerWithKnownService(serviceUri: JavaURI, maxAge: Option[Int] = None)(thunk: => Unit)(implicit system: ActorSystem): Unit = {
    import system.dispatcher
    implicit val materializer = ActorMaterializer.create(system)

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
