package com.typesafe.conductr.clientlib.akka

import java.net.URL

import akka.actor.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.client.RequestBuilding._
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import akka.testkit.TestActor.AutoPilot
import akka.testkit.{ TestActor, TestProbe }
import com.typesafe.conductr.clientlib.akka.TestData._
import com.typesafe.conductr.lib.AkkaUnitTest
import de.heikoseeberger.akkasse.{ ServerSentEvent, EventStreamMarshalling }
import scala.concurrent.duration._
import scala.collection.immutable._

import scala.concurrent.Await

class BundlesConnectorSpec extends AkkaUnitTest {
  "should emit bundles retrieved on the initial connection and subsequent bundle changes" in {
    val f = testFixture("127.0.0.1", 6667)
    import f._

    val bundleStreamMonitor = TestProbe()

    Source.single(Get("/v2/bundles/events") -> Get("/v2/bundles"))
      .via(BundlesConnector.connect(serverAddress))
      .runForeach(bundleStreamMonitor.ref ! _)

    // Initial bundle state should be emitted
    getBundles.expectMsg(ControlClientSpec.GetBundles)
    getBundles.reply(s"[${TestData.BundleBackendAsJson}]")

    bundleStreamMonitor.expectMsg(Seq(TestData.BundleBackend))

    // Bundle Events should be requested
    getBundleEvents.expectMsg(ControlClientSpec.GetBundleEvents)
    getBundleEvents.reply(
      Source.tick(
        initialDelay = 100.millis,
        interval = 800.millis,
        tick = Seq(
          ServerSentEvent(s"${BundleFrontend.bundleId}", "bundleInstallationAdded"),
          ServerSentEvent.Heartbeat,
          ServerSentEvent.Heartbeat,
          ServerSentEvent.Heartbeat,
          ServerSentEvent.Heartbeat
        )
      ).mapConcat(identity)
    )

    // Simulate no changes within bundle state
    getBundles.expectMsg(ControlClientSpec.GetBundles)
    getBundles.reply(s"[${TestData.BundleBackendAsJson}]")

    bundleStreamMonitor.expectNoMsg(500.millis)

    // Simulate changes within bundle state
    getBundles.expectMsg(ControlClientSpec.GetBundles)
    getBundles.reply(s"[${TestData.BundleBackendAsJson}, ${TestData.BundleFrontendAsJson}]")

    bundleStreamMonitor.expectMsg(Seq(TestData.BundleBackend, TestData.BundleFrontend))

    // Simulate no changes within bundle state
    getBundles.expectMsg(ControlClientSpec.GetBundles)
    getBundles.reply(s"[${TestData.BundleBackendAsJson}, ${TestData.BundleFrontendAsJson}]")

    bundleStreamMonitor.expectNoMsg(500.millis)
  }

  "should fail with timeout error if source does not complete within specified timeout" in {
    val f = testFixture("127.0.0.1", 6668)
    import f._

    getBundles.setAutoPilot(new TestActor.AutoPilot {
      override def run(sender: ActorRef, msg: Any): AutoPilot = {
        msg shouldBe ControlClientSpec.GetBundles
        sender ! "[]"
        keepRunning
      }
    })

    val result = Source.single(Get("/v2/bundles/events") -> Get("/v2/bundles"))
      .via(BundlesConnector.connect(serverAddress, stopAfter = Some(timeout.duration / 2)))
      .runWith(Sink.ignore)

    // Bundle Events should be requested
    getBundleEvents.expectMsg(ControlClientSpec.GetBundleEvents)
    getBundleEvents.reply(
      Source.tick(
        initialDelay = 100.millis,
        interval = 800.millis,
        tick = Seq(
          ServerSentEvent(s"${BundleFrontend.bundleId}", "bundleInstallationAdded"),
          ServerSentEvent.Heartbeat,
          ServerSentEvent.Heartbeat,
          ServerSentEvent.Heartbeat,
          ServerSentEvent.Heartbeat
        )
      ).mapConcat(identity)
    )

    val error = intercept[RuntimeException] {
      Await.result(result, timeout.duration)
    }
    error shouldBe BundlesConnector.TimeoutException
  }

  def testFixture(serverHost: String, serverPort: Int) = new {
    implicit val dispatcher = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val getBundleEvents = TestProbe()
    val getBundles = TestProbe()

    // format: OFF
    val route = pathPrefix("v2" / "bundles") {
      path("events") {
        get {
          complete {
            import EventStreamMarshalling._
            getBundleEvents.ref.ask(ControlClientSpec.GetBundleEvents)
              .mapTo[Source[ServerSentEvent, _]]
              .map(Marshal(_).to[HttpResponse])
          }
        }
      } ~
      get {
        complete {
          getBundles.ref.ask(ControlClientSpec.GetBundles)
            .mapTo[String]
            .map { json =>
              HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, json))
            }
        }
      }
    }
    // format: ON

    val serverAddress = new URL(s"http://$serverHost:$serverPort")
    Await.ready(
      Http().bindAndHandle(route, serverHost, serverPort),
      timeout.duration
    )
  }
}
