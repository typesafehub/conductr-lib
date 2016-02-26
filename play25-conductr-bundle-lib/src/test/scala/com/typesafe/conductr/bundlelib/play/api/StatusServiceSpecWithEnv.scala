package com.typesafe.conductr.bundlelib.play.api

import java.net.{ InetSocketAddress, URL }

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.testkit.TestProbe
import com.typesafe.conductr.lib.AkkaUnitTestWithFixture
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._

import scala.concurrent.Await
import scala.util.{ Failure, Success }

class StatusServiceSpecWithEnv extends AkkaUnitTestWithFixture("StatusServiceSpecWithEnv") {

  def systemFixture(f: this.FixtureParam) = new {
    implicit val system = f.system
    implicit val mat = ActorMaterializer()
    implicit val timeout = f.timeout
  }

  "The StatusService functionality in the library" should {
    "be able to call the right URL to signal that it is up" in { f =>
      val sys = systemFixture(f)
      import sys._

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

      val url = new URL(Env.conductRStatus.get)
      val server = Http(system).bindAndHandle(handler, url.getHost, url.getPort)

      import system.dispatcher

      try {
        server.onComplete {
          case Success(binding) => probe.ref ! binding.localAddress
          case Failure(e)       => probe.ref ! e
        }

        val address = probe.expectMsgType[InetSocketAddress]
        address.getHostString should be(url.getHost)
        address.getPort should be(url.getPort)

        val app = new GuiceApplicationBuilder()
          .bindings(new BundlelibModule)
          .disable(classOf[ConductRLifecycleModule])
          .build()
        running(app) {
          val statusService = app.injector.instanceOf(classOf[StatusService])
          Await.result(statusService.signalStarted(), timeout.duration).isDefined shouldBe true
        }

        val receivedId = probe.expectMsgType[String]
        receivedId should be(Env.bundleId.get)

      } finally {
        server.foreach(_.unbind())
      }
    }
  }
}
