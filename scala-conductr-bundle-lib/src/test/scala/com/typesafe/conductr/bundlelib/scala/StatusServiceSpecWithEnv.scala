package com.typesafe.conductr.bundlelib.scala

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import com.typesafe.conductr.AkkaUnitTest
import java.net.InetSocketAddress
import com.typesafe.conductr.scala.ConnectionContext.Implicits

import scala.concurrent.Await
import scala.util.{ Failure, Success }

class StatusServiceSpecWithEnv extends AkkaUnitTest("StatusServiceSpecWithEnv", "akka.loglevel = INFO") {

  import Implicits.global

  "The StatusService functionality in the library" should {
    "be able to call the right URL to signal that it is up" in {

      import system.dispatcher
      implicit val materializer = ActorMaterializer()

      val probe = new TestProbe(system)

      val handler =
        path("bundles" / Segment) { bundleId =>
          put {
            parameter('isStarted ! "true") {
              complete {
                probe.ref ! bundleId
                StatusCodes.NoContent
              }
            }
          }
        }

      val url = URL(Env.conductRStatus.get)
      val server = Http(system).bindAndHandle(handler, url.getHost, url.getPort)

      try {
        server.onComplete {
          case Success(binding) => probe.ref ! binding.localAddress
          case Failure(e)       => probe.ref ! e
        }

        val address = probe.expectMsgType[InetSocketAddress]
        address.getHostString should be(url.getHost)
        address.getPort should be(url.getPort)

        Await.result(StatusService.signalStarted(), timeout.duration).isDefined shouldBe true

        val receivedId = probe.expectMsgType[String]
        receivedId should be(Env.bundleId.get)

      } finally {
        server.foreach(_.unbind())
      }
    }
  }
}
