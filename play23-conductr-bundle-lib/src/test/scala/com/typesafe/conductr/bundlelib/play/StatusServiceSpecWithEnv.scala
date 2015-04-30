package com.typesafe.conductr.bundlelib.play

import java.net.{ InetSocketAddress, URL }

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorFlowMaterializer
import akka.testkit.TestProbe
import com.typesafe.conductr.bundlelib.play.ConnectionContext.Implicits
import com.typesafe.conductr.{ AkkaUnitTest, _ }

import scala.concurrent.Await
import scala.util.{ Failure, Success }

class StatusServiceSpecWithEnv extends AkkaUnitTest("StatusServiceSpecWithEnv", "akka.loglevel = INFO") {

  "The StatusService functionality in the library" should {
    "be able to call the right URL to signal that it is up" in {

      val probe = new TestProbe(system)

      import Implicits.defaultContext

      import system.dispatcher
      implicit val materializer = ActorFlowMaterializer()

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

      val url = new URL(Env.conductRStatus.get)
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
