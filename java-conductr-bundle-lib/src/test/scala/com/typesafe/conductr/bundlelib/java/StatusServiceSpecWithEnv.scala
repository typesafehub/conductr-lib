package com.typesafe.conductr.bundlelib.java

import java.net.{ URL, InetSocketAddress }

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import com.typesafe.conductr.AkkaUnitTest
import com.typesafe.conductr.java.Await

import scala.util.{ Failure, Success }

class StatusServiceSpecWithEnv extends AkkaUnitTest("StatusServiceSpecWithEnv", "akka.loglevel = INFO") {

  import scala.concurrent.ExecutionContext.Implicits.global

  "The StatusService functionality in the library" should {
    "be able to call the right URL to signal that it is up" in {
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

      val url = new URL(Env.CONDUCTR_STATUS.get)
      val server = Http(system).bindAndHandle(handler, url.getHost, url.getPort)

      try {
        server.onComplete {
          case Success(binding) => probe.ref ! binding.localAddress
          case Failure(e)       => probe.ref ! e
        }

        val address = probe.expectMsgType[InetSocketAddress]
        address.getHostString should be(url.getHost)
        address.getPort should be(url.getPort)

        Await.result(StatusService.signalStarted(), timeout.duration).isPresent shouldBe true

        val receivedId = probe.expectMsgType[String]
        receivedId should be(Env.BUNDLE_ID.get)

      } finally {
        server.foreach(_.unbind())
      }
    }
  }
}
