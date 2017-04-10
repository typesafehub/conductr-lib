package com.typesafe.conductr.clientlib.akka

import java.io.File
import java.net.URL

import akka.actor.ActorDSL._
import akka.actor.{ ActorRef, ActorSystem }
import akka.contrib.http.Directives._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpEntity.IndefiniteLength
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{ FileIO, Flow, Keep, Sink, Source }
import akka.testkit.TestActor.AutoPilot
import akka.testkit.TestProbe
import akka.util.{ ByteString, Timeout }
import com.typesafe.conductr.lib.AkkaUnitTestWithFixture
import com.typesafe.conductr.lib.akka.ConnectionContext
import com.typesafe.conductr.clientlib.akka.models.{ EventStreamFailure, EventStreamSuccess }
import com.typesafe.conductr.clientlib.scala.models._

import de.heikoseeberger.akkasse.{ EventStreamMarshalling, ServerSentEvent }
import org.reactivestreams.Publisher
import org.scalatest.Inside
import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future, Await }
import akka.http.scaladsl.model._
import scala.util.{ Failure, Success }
import scala.collection.JavaConverters._

object ControlClientSpec {
  val BundleDescriptorHocon =
    """
      |compatibilityVersion="1"
      |components {
      |  eslite {
      |    description = eslite
      |    endpoints {
      |      akka-remote {
      |        bind-port = 0
      |        bind-protocol = tcp
      |        services = []
      |      }
      |      es {
      |        bind-port = 0
      |        bind-protocol = http
      |        service-name = elastic-search
      |        services = []
      |      }
      |    }
      |    file-system-type = universal
      |    start-command = [
      |      "eslite/bin/eslite",
      |      "-J-Xms134217728",
      |      "-J-Xmx134217728",
      |      "-Dhttp.address=$ES_BIND_IP",
      |      "-Dhttp.port=$ES_BIND_PORT",
      |      "-Dplay.crypto.secret=65736c697465"
      |    ]
      |  }
      |}
      |diskSpace = 200000000
      |memory = 402653184
      |name = eslite
      |nrOfCpus = 0.1
      |roles = [elasticsearch]
      |system = eslite
      |systemVersion = "1"
      |version = "1"
    """.stripMargin

  val BundleDescriptorFromHocon =
    BundleDescriptor(
      version = "1",
      system = "eslite",
      systemVersion = "1",
      nrOfCpus = 0.1,
      memory = 402653184L,
      diskSpace = 200000000L,
      roles = Seq("elasticsearch"),
      bundleName = "eslite",
      compatibilityVersion = "1",
      tags = Seq.empty,
      annotations = None,
      components = Map(
        "eslite" -> new BundleDescriptor.Component(
          "eslite",
          BundleDescriptor.Component.FileSystemType.Universal,
          Seq(
            "eslite/bin/eslite",
            "-J-Xms134217728",
            "-J-Xmx134217728",
            "-Dhttp.address=$ES_BIND_IP",
            "-Dhttp.port=$ES_BIND_PORT",
            "-Dplay.crypto.secret=65736c697465"
          ),
          Map(
            "akka-remote" -> BundleDescriptor.Component.Endpoint("tcp", 0, None, Seq.empty),
            "es" -> BundleDescriptor.Component.Endpoint("http", 0, Some("elastic-search"), Seq.empty)
          )
        )
      )
    )

  case object GetBundleEvents
  case object GetBundles

  def writeToFile(file: File, text: String)(implicit mat: ActorMaterializer, timeout: Timeout): Unit =
    Await.result(
      Source.single(ByteString.fromString(text)).runWith(FileIO.toPath(file.toPath)),
      timeout.duration
    )

  def extractFromBodyPart(bodyPart: Multipart.FormData.BodyPart)(implicit mat: ActorMaterializer, ec: ExecutionContext): (String, String) = {
    bodyPart.entity.dataBytes.runWith(Sink.ignore)
    val name = bodyPart.name
    val fileName = bodyPart.contentDispositionHeader.flatMap(_.params.get("filename")).getOrElse("n/a")
    (name, fileName)
  }

  def extractFromMultipartForm(formData: Multipart.FormData, expectedLength: Int)(implicit mat: ActorMaterializer, ec: ExecutionContext): Future[Seq[(String, String)]] =
    for {
      parts <- formData.parts.prefixAndTail(expectedLength).runWith(Sink.head)
    } yield {
      val (expectedParts, _) = parts
      expectedParts.map(extractFromBodyPart)
    }

  def toByteArrayPublisher(input: String)(implicit mat: ActorMaterializer): Publisher[Array[Byte]] =
    Source.single(input).map(_.getBytes).runWith(Sink.asPublisher(fanout = false))
}

class ControlClientSpec extends AkkaUnitTestWithFixture("ControlClientSpec") with Inside {

  import TestData._
  import Directives._
  import JsonMarshalling._
  import EventStreamMarshalling._

  val ApiVersion = "v2"

  def systemFixture(f: this.FixtureParam) = new {
    implicit val system = f.system
    implicit val cc: ConnectionContext = ConnectionContext()
    implicit val mat = cc.actorMaterializer
    implicit val ec = cc.actorMaterializer.executionContext
    implicit val timeout = f.timeout
    implicit val HostUrl = new URL(s"http://127.0.0.1:5555")
  }

  "ControlClient" should {

    "retrieve information of all bundles" in { f =>
      val sys = systemFixture(f)
      import sys._

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
          get {
            complete {
              HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,
                s"""
                   |[
                   |  $BundleFrontendAsJson,
                   |  $BundleBackendAsJson
                   |]
                """.stripMargin)
              )
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).getBundlesInfo(), timeout.duration) shouldBe Seq(BundleFrontend, BundleBackend)
      }
    }

    "get bundle and config" in { f =>
      val sys = systemFixture(f)
      import sys._

      val bundleFile = File.createTempFile("bundle-1", ".zip")
      ControlClientSpec.writeToFile(bundleFile, "bundle zip file")

      val configFile = File.createTempFile("config-1", ".zip")
      ControlClientSpec.writeToFile(configFile, "bundle configuration zip file")

      val routeInputMonitor = TestProbe()

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles" / Segment) { bundleId =>
          get {
            accept(MediaTypes.`multipart/form-data`) {
              complete {
                routeInputMonitor.ref ! bundleId
                Marshal(
                  Multipart.FormData(
                    Multipart.FormData.BodyPart(
                      "bundle",
                      IndefiniteLength(MediaTypes.`application/octet-stream`, FileIO.fromPath(bundleFile.toPath)),
                      Map("filename" -> bundleFile.getName)
                    ),
                    Multipart.FormData.BodyPart(
                      "configuration",
                      IndefiniteLength(MediaTypes.`application/octet-stream`, FileIO.fromPath(configFile.toPath)),
                      Map("filename" -> configFile.getName)
                    )
                  )
                ).to[HttpResponse]
              }
            }
          }
        }
      // format: ON

      val (bundleDataSubscriber, bundleData) = Source.asSubscriber[Array[Byte]].map(new String(_)).toMat(Sink.fold("")(_ + _))(Keep.both).run()
      val (configDataSubscriber, configData) = Source.asSubscriber[Array[Byte]].map(new String(_)).toMat(Sink.fold("")(_ + _))(Keep.both).run()

      withServer(route) {
        val result = Await.result(ControlClient(HostUrl).getBundle("vis", bundleDataSubscriber, configDataSubscriber), timeout.duration)
        inside(result) {
          case v: BundleGetSuccess =>
            v.bundleId shouldBe "vis"
            v.bundleFileName shouldBe bundleFile.getName
            v.configFileName.get shouldBe configFile.getName
            Await.result(bundleData, timeout.duration) shouldBe "bundle zip file"
            Await.result(configData, timeout.duration) shouldBe "bundle configuration zip file"
        }
        routeInputMonitor.expectMsg("vis")
      }
    }

    "get bundle only" in { f =>
      val sys = systemFixture(f)
      import sys._

      val bundleFile = File.createTempFile("bundle-1", ".zip")
      ControlClientSpec.writeToFile(bundleFile, "bundle zip file")

      val routeInputMonitor = TestProbe()

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles" / Segment) { bundleId =>
          get {
            accept(MediaTypes.`multipart/form-data`) {
              complete {
                routeInputMonitor.ref ! bundleId
                Marshal(
                  Multipart.FormData(
                    Multipart.FormData.BodyPart(
                      "bundle",
                      IndefiniteLength(MediaTypes.`application/octet-stream`, FileIO.fromPath(bundleFile.toPath)),
                      Map("filename" -> bundleFile.getName)
                    )
                  )
                ).to[HttpResponse]
              }
            }
          }
        }
      // format: ON

      val (bundleDataSubscriber, bundleData) = Source.asSubscriber[Array[Byte]].map(new String(_)).toMat(Sink.fold("")(_ + _))(Keep.both).run()
      val (configDataSubscriber, configData) = Source.asSubscriber[Array[Byte]].map(new String(_)).toMat(Sink.fold("")(_ + _))(Keep.both).run()

      withServer(route) {

        val result = Await.result(ControlClient(HostUrl).getBundle("vis", bundleDataSubscriber, configDataSubscriber), timeout.duration)
        inside(result) {
          case v: BundleGetSuccess =>
            v.bundleId shouldBe "vis"
            v.bundleFileName shouldBe bundleFile.getName
            v.configFileName shouldBe None
            Await.result(bundleData, timeout.duration) shouldBe "bundle zip file"
            Await.result(configData, timeout.duration) shouldBe ""
        }
        routeInputMonitor.expectMsg("vis")
      }
    }

    "get bundle returning a failure result when encountering invalid response" in { f =>
      val sys = systemFixture(f)
      import sys._

      val bundleFile = File.createTempFile("bundle-1", ".zip")
      val configFile = File.createTempFile("config-1", ".zip")
      val routeInputMonitor = TestProbe()

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles" / Segment) { bundleId =>
          get {
            accept(MediaTypes.`multipart/form-data`) {
              complete {
                routeInputMonitor.ref ! bundleId
                Marshal(
                  Multipart.FormData(
                    Multipart.FormData.BodyPart(
                      "foo",
                      IndefiniteLength(MediaTypes.`application/octet-stream`, FileIO.fromPath(bundleFile.toPath)),
                      Map("filename" -> bundleFile.getName)
                    ),
                    Multipart.FormData.BodyPart(
                      "bar",
                      IndefiniteLength(MediaTypes.`application/octet-stream`, FileIO.fromPath(configFile.toPath)),
                      Map("filename" -> configFile.getName)
                    )
                  )
                ).to[HttpResponse]
              }
            }
          }
        }
      // format: ON

      val (bundleDataSubscriber, bundleData) = Source.asSubscriber[Array[Byte]].map(new String(_)).toMat(Sink.fold("")(_ + _))(Keep.both).run()
      val (configDataSubscriber, configData) = Source.asSubscriber[Array[Byte]].map(new String(_)).toMat(Sink.fold("")(_ + _))(Keep.both).run()

      withServer(route) {
        intercept[InvalidBundleGetResponseBody] {
          Await.result(ControlClient(HostUrl).getBundle("vis", bundleDataSubscriber, configDataSubscriber), timeout.duration)
        }
        intercept[InvalidBundleGetResponseBody] {
          Await.result(bundleData, timeout.duration)
        }
        intercept[InvalidBundleGetResponseBody] {
          Await.result(configData, timeout.duration)
        }

        routeInputMonitor.expectMsg("vis")
      }
    }

    "get bundle returning a failure result when encountering http error" in { f =>
      val sys = systemFixture(f)
      import sys._

      val routeInputMonitor = TestProbe()

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles" / Segment) { bundleId =>
          get {
            routeInputMonitor.ref ! bundleId
            accept(MediaTypes.`multipart/form-data`) {
              complete {
                HttpResponse(status = StatusCodes.InternalServerError, entity = HttpEntity("test error"))
              }
            }
          }
        }
      // format: ON

      val (bundleDataSubscriber, bundleData) = Source.asSubscriber[Array[Byte]].map(new String(_)).toMat(Sink.fold("")(_ + _))(Keep.both).run()
      val (configDataSubscriber, configData) = Source.asSubscriber[Array[Byte]].map(new String(_)).toMat(Sink.fold("")(_ + _))(Keep.both).run()

      withServer(route) {
        Await.result(ControlClient(HostUrl).getBundle("vis", bundleDataSubscriber, configDataSubscriber), timeout.duration) shouldBe BundleGetFailure(500, "test error")
        intercept[RuntimeException] {
          Await.result(bundleData, timeout.duration)
        }
        intercept[RuntimeException] {
          Await.result(configData, timeout.duration)
        }
        routeInputMonitor.expectMsg("vis")
      }
    }

    "get bundle descriptor returning success" in { f =>
      import ControlClientSpec._

      val sys = systemFixture(f)
      import sys._

      val routeInputMonitor = TestProbe()
      val `application/hocon` = MediaType.applicationWithFixedCharset("hocon", HttpCharsets.`UTF-8`)

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles" / Segment) { bundleId =>
          get {
            routeInputMonitor.ref ! bundleId
            accept(`application/hocon`) {
              complete {
                HttpResponse(status = StatusCodes.OK, entity = HttpEntity(BundleDescriptorHocon))
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        val result = Await.result(ControlClient(HostUrl).getBundleDescriptor("vis"), timeout.duration)
        inside(result) {
          case BundleGetDescriptorSuccess(descriptor) =>
            descriptor shouldBe BundleDescriptorFromHocon
        }
        routeInputMonitor.expectMsg("vis")
      }
    }

    "get bundle descriptor config returning failure" in { f =>
      val sys = systemFixture(f)
      import sys._

      val routeInputMonitor = TestProbe()
      val `application/hocon` = MediaType.applicationWithFixedCharset("hocon", HttpCharsets.`UTF-8`)

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles" / Segment) { bundleId =>
          get {
            routeInputMonitor.ref ! bundleId
            accept(`application/hocon`) {
              complete {
                HttpResponse(status = StatusCodes.InternalServerError, entity = HttpEntity("test error"))
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        val result = Await.result(ControlClient(HostUrl).getBundleDescriptorConfig("vis"), timeout.duration)
        result shouldBe BundleGetDescriptorConfigFailure(500, "test error")
        routeInputMonitor.expectMsg("vis")
      }

    }

    "get bundle descriptor config returning success" in { f =>
      val sys = systemFixture(f)
      import sys._

      val routeInputMonitor = TestProbe()
      val `application/hocon` = MediaType.applicationWithFixedCharset("hocon", HttpCharsets.`UTF-8`)

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles" / Segment) { bundleId =>
          get {
            routeInputMonitor.ref ! bundleId
            accept(`application/hocon`) {
              complete {
                HttpResponse(status = StatusCodes.OK, entity = HttpEntity("a = b"))
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        val result = Await.result(ControlClient(HostUrl).getBundleDescriptorConfig("vis"), timeout.duration)
        inside(result) {
          case BundleGetDescriptorConfigSuccess(config) =>
            config.unwrapped().asScala shouldBe Map("a" -> "b")
        }
        routeInputMonitor.expectMsg("vis")
      }
    }

    "get bundle descriptor returning failure" in { f =>
      val sys = systemFixture(f)
      import sys._

      val routeInputMonitor = TestProbe()
      val `application/hocon` = MediaType.applicationWithFixedCharset("hocon", HttpCharsets.`UTF-8`)

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles" / Segment) { bundleId =>
          get {
            routeInputMonitor.ref ! bundleId
            accept(`application/hocon`) {
              complete {
                HttpResponse(status = StatusCodes.InternalServerError, entity = HttpEntity("test error"))
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        val result = Await.result(ControlClient(HostUrl).getBundleDescriptor("vis"), timeout.duration)
        result shouldBe BundleGetDescriptorFailure(500, "test error")
        routeInputMonitor.expectMsg("vis")
      }

    }

    "load a valid bundle" in { f =>
      val sys = systemFixture(f)
      import sys._

      val bodyPartsMonitor = TestProbe()

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
          post {
            extractRequest { request =>
              complete {
              for {
                formData <- Unmarshal(request.entity).to[Multipart.FormData]
                expectedBodyParts <- ControlClientSpec.extractFromMultipartForm(formData, expectedLength = 2)
              } yield {
                bodyPartsMonitor.ref ! expectedBodyParts
                HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,
                  s"""
                     |{
                     |  "requestId": "$RequestId",
                     |  "bundleId": "${BundleFrontend.bundleId}"
                     |}
                  """.stripMargin)
                )
              }
            }
            }
          }
        }
      // format: ON

      withServer(route) {
        val request = ControlClient(HostUrl).loadBundle(BundleUri, None)
        Await.result(request, timeout.duration * 2) shouldBe BundleRequestSuccess(RequestId, BundleFrontend.bundleId)

        bodyPartsMonitor.expectMsg(Seq(
          ("bundleConf", "bundle.conf"),
          ("bundle", BundleFileName)
        ))
      }
    }

    "load a valid bundle + config overlay + configuration" in { f =>
      val sys = systemFixture(f)
      import sys._

      val bodyPartsMonitor = TestProbe()

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
          post {
            extractRequest { request =>
              complete {
              for {
                formData <- Unmarshal(request.entity).to[Multipart.FormData]
                expectedBodyParts <- ControlClientSpec.extractFromMultipartForm(formData, expectedLength = 4)
              } yield {
                bodyPartsMonitor.ref ! expectedBodyParts
                HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,
                  s"""
                     |{
                     |  "requestId": "$RequestId",
                     |  "bundleId": "${BundleFrontend.bundleId}"
                     |}
                  """.stripMargin)
                )
              }
            }
            }
          }
        }
      // format: ON

      withServer(route) {
        val request = ControlClient(HostUrl).loadBundle(
          bundleConf = ControlClientSpec.toByteArrayPublisher("bundle.conf"),
          bundleConfOverlay = Some(ControlClientSpec.toByteArrayPublisher("bundle.conf overlay")),
          bundle = BundleFile("bundle-1.zip", ControlClientSpec.toByteArrayPublisher("bundle zip file")),
          config = Some(BundleConfigurationFile("config-1.zip", ControlClientSpec.toByteArrayPublisher("config zip file")))
        )
        Await.result(request, timeout.duration * 2) shouldBe BundleRequestSuccess(RequestId, BundleFrontend.bundleId)

        bodyPartsMonitor.expectMsg(Seq(
          ("bundleConf", "bundle.conf"),
          ("bundleConfOverlay", "bundle.conf"),
          ("bundle", "bundle-1.zip"),
          ("configuration", "config-1.zip")
        ))
      }
    }

    "load a valid bundle + config overlay + configuration which completes when bundle is installed" in { f =>
      val sys = systemFixture(f)
      import sys._

      val bodyPartsMonitor = TestProbe()

      val getBundleEvents = TestProbe()
      val getBundles = TestProbe()

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
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
          post {
            extractRequest { request =>
              complete {
              for {
                formData <- Unmarshal(request.entity).to[Multipart.FormData]
                expectedBodyParts <- ControlClientSpec.extractFromMultipartForm(formData, expectedLength = 4)
              } yield {
                bodyPartsMonitor.ref ! expectedBodyParts
                HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,
                  s"""
                     |{
                     |  "requestId": "$RequestId",
                     |  "bundleId": "${BundleFrontend.bundleId}"
                     |}
                  """.stripMargin)
                )
              }
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

      withServer(route) {
        val request = ControlClient(HostUrl).loadBundleComplete(
          bundleConf = ControlClientSpec.toByteArrayPublisher("bundle.conf"),
          bundleConfOverlay = Some(ControlClientSpec.toByteArrayPublisher("bundle.conf overlay")),
          bundle = BundleFile("bundle-1.zip", ControlClientSpec.toByteArrayPublisher("bundle zip file")),
          config = Some(BundleConfigurationFile("config-1.zip", ControlClientSpec.toByteArrayPublisher("config zip file")))
        )

        // Simulate bundle not installed
        getBundles.expectMsg(ControlClientSpec.GetBundles)
        getBundles.reply("[]")
        request.isCompleted shouldBe false

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

        // Simulate no bundle being installed just yet
        getBundles.expectMsg(ControlClientSpec.GetBundles)
        getBundles.reply("[]")
        request.isCompleted shouldBe false

        // And then simulate bundle installed
        getBundles.expectMsg(ControlClientSpec.GetBundles)
        getBundles.reply(s"[$BundleFrontendAsJson]")

        Await.result(request, timeout.duration * 2) shouldBe BundleRequestSuccess(RequestId, BundleFrontend.bundleId)

        bodyPartsMonitor.expectMsg(Seq(
          ("bundleConf", "bundle.conf"),
          ("bundleConfOverlay", "bundle.conf"),
          ("bundle", "bundle-1.zip"),
          ("configuration", "config-1.zip")
        ))
      }
    }

    "load a valid bundle + config overlay + configuration which timed out waiting for bundle installed" in { f =>
      val sys = systemFixture(f)
      import sys._

      val bodyPartsMonitor = TestProbe()

      val getBundleEvents = TestProbe()
      val getBundles = TestProbe()

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
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
          post {
            extractRequest { request =>
              complete {
              for {
                formData <- Unmarshal(request.entity).to[Multipart.FormData]
                expectedBodyParts <- ControlClientSpec.extractFromMultipartForm(formData, expectedLength = 4)
              } yield {
                bodyPartsMonitor.ref ! expectedBodyParts
                HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,
                  s"""
                     |{
                     |  "requestId": "$RequestId",
                     |  "bundleId": "${BundleFrontend.bundleId}"
                     |}
                  """.stripMargin)
                )
              }
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

      withServer(route) {
        val request = ControlClient(HostUrl).loadBundleComplete(
          bundleConf = ControlClientSpec.toByteArrayPublisher("bundle.conf"),
          bundleConfOverlay = Some(ControlClientSpec.toByteArrayPublisher("bundle.conf overlay")),
          bundle = BundleFile("bundle-1.zip", ControlClientSpec.toByteArrayPublisher("bundle zip file")),
          config = Some(BundleConfigurationFile("config-1.zip", ControlClientSpec.toByteArrayPublisher("config zip file"))),
          completeTimeout = timeout.duration / 2
        )

        // Simulate bundle is never installed
        getBundles.setAutoPilot(new AutoPilot {
          override def run(sender: ActorRef, msg: Any): AutoPilot = {
            msg shouldBe ControlClientSpec.GetBundles
            sender ! "[]"
            keepRunning
          }
        })

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

        intercept[BundleRequestTimedOut] {
          Await.result(request, timeout.duration)
        }

        bodyPartsMonitor.expectMsg(Seq(
          ("bundleConf", "bundle.conf"),
          ("bundleConfOverlay", "bundle.conf"),
          ("bundle", "bundle-1.zip"),
          ("configuration", "config-1.zip")
        ))
      }
    }

    "handle error when loading an invalid bundle" in { f =>
      val sys = systemFixture(f)
      import sys._

      val errorMessage = s"There was a problem scheduling the request (request id: '$RequestId')"

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
          post {
            complete(StatusCodes.BadRequest, errorMessage)
          }
        }
      // format: ON

      withServer(route) {
        val request = ControlClient(HostUrl).loadBundle(BundleUri, None)
        Await.result(request, timeout.duration) shouldBe BundleRequestFailure(400, errorMessage)
      }
    }

    "run a bundle" in { f =>
      val sys = systemFixture(f)
      import sys._

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
          path(Segment) { bundleId =>
            put {
              parameter('scale.as[Int]) { scale =>
                complete {
                  HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,
                    s"""
                       |{
                       |  "requestId": "$RequestId",
                       |  "bundleId": "${BundleFrontend.bundleId}"
                       |}
                   """.stripMargin)
                  )
                }
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).runBundle(BundleFrontend.bundleId), timeout.duration) shouldBe BundleRequestSuccess(RequestId, BundleFrontend.bundleId)
      }
    }

    "run a bundle which completes when desired bundle scale is achieved" in { f =>
      val sys = systemFixture(f)
      import sys._

      val getBundleEvents = TestProbe()
      val getBundles = TestProbe()

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
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
          } ~
          path(Segment) { bundleId =>
            put {
              parameter('scale.as[Int]) { scale =>
                complete {
                  HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,
                    s"""
                       |{
                       |  "requestId": "$RequestId",
                       |  "bundleId": "${BundleFrontend.bundleId}"
                       |}
                   """.stripMargin)
                  )
                }
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        val request = ControlClient(HostUrl).runBundleComplete(BundleFrontend.bundleId)

        // Simulate bundle not running
        getBundles.expectMsg(ControlClientSpec.GetBundles)
        getBundles.reply(s"[$BundleFrontendNoExecutionAsJson]")
        request.isCompleted shouldBe false

        // Bundle Events should be requested
        getBundleEvents.expectMsg(ControlClientSpec.GetBundleEvents)
        getBundleEvents.reply(
          Source.tick(
            initialDelay = 100.millis,
            interval = 800.millis,
            tick = Seq(
              ServerSentEvent(s"${BundleFrontend.bundleId}", "bundleExecutionAdded"),
              ServerSentEvent.Heartbeat,
              ServerSentEvent.Heartbeat,
              ServerSentEvent.Heartbeat,
              ServerSentEvent.Heartbeat
            )
          ).mapConcat(identity)
        )

        // Simulate no bundle running just yet
        getBundles.expectMsg(ControlClientSpec.GetBundles)
        getBundles.reply(s"[$BundleFrontendNoExecutionAsJson]")
        request.isCompleted shouldBe false

        // And then simulate bundle running
        getBundles.expectMsg(ControlClientSpec.GetBundles)
        getBundles.reply(s"[$BundleFrontendAsJson]")

        Await.result(request, timeout.duration) shouldBe BundleRequestSuccess(RequestId, BundleFrontend.bundleId)
      }
    }

    "run a bundle which times out waiting for desired bundle scale to be achieved" in { f =>
      val sys = systemFixture(f)
      import sys._

      val getBundleEvents = TestProbe()
      val getBundles = TestProbe()

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
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
          } ~
          path(Segment) { bundleId =>
            put {
              parameter('scale.as[Int]) { scale =>
                complete {
                  HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,
                    s"""
                       |{
                       |  "requestId": "$RequestId",
                       |  "bundleId": "${BundleFrontend.bundleId}"
                       |}
                   """.stripMargin)
                  )
                }
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        val request = ControlClient(HostUrl).runBundleComplete(BundleFrontend.bundleId, completeTimeout = timeout.duration / 2)

        // Simulate bundle desired scale never achieved
        getBundles.setAutoPilot(new AutoPilot {
          override def run(sender: ActorRef, msg: Any): AutoPilot = {
            msg shouldBe ControlClientSpec.GetBundles
            sender ! s"[$BundleFrontendNoExecutionAsJson]"
            keepRunning
          }
        })

        // Bundle Events should be requested
        getBundleEvents.expectMsg(ControlClientSpec.GetBundleEvents)
        getBundleEvents.reply(
          Source.tick(
            initialDelay = 100.millis,
            interval = 800.millis,
            tick = Seq(
              ServerSentEvent(s"${BundleFrontend.bundleId}", "bundleExecutionAdded"),
              ServerSentEvent.Heartbeat,
              ServerSentEvent.Heartbeat,
              ServerSentEvent.Heartbeat,
              ServerSentEvent.Heartbeat
            )
          ).mapConcat(identity)
        )

        intercept[BundleRequestTimedOut] {
          Await.result(request, timeout.duration)
        }
      }
    }

    "handle error when running a bundle" in { f =>
      val sys = systemFixture(f)
      import sys._

      val errorMessage = s"Specified Bundle ID/name: '${BundleFrontend.bundleId}' resulted in multiple Bundle IDs: 'ab37fd, 23bf8a'"

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
          path(Segment) { bundleId =>
            put {
              parameter('scale.as[Int]) { scale =>
                complete(StatusCodes.MultipleChoices, errorMessage)
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).runBundle(BundleFrontend.bundleId), timeout.duration) shouldBe BundleRequestFailure(300, errorMessage)
      }
    }

    "stop a bundle" in { f =>
      val sys = systemFixture(f)
      import sys._

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
          path(Segment) { bundleId =>
            put {
              parameter('scale.as[Int]) { scale =>
                complete {
                  HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,
                    s"""
                       |{
                       |  "requestId": "$RequestId",
                       |  "bundleId": "${BundleFrontend.bundleId}"
                       |}
                   """.stripMargin)
                  )
                }
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).stopBundle(BundleFrontend.bundleId), timeout.duration) shouldBe BundleRequestSuccess(RequestId, BundleFrontend.bundleId)
      }
    }

    "handle error when stopping a bundle" in { f =>
      val sys = systemFixture(f)
      import sys._

      val errorMessage = s"No bundle found by the specified Bundle ID/name: '${BundleFrontend.bundleId}'"

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
          path(Segment) { bundleId =>
            put {
              parameter('scale.as[Int]) { scale =>
                complete(StatusCodes.NotFound, errorMessage)
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).stopBundle(BundleFrontend.bundleId), timeout.duration) shouldBe BundleRequestFailure(404, errorMessage)
      }
    }

    "unload a bundle" in { f =>
      val sys = systemFixture(f)
      import sys._

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
          path(Segment) { bundleId =>
            delete {
              complete {
                HttpResponse(entity = HttpEntity(ContentTypes.`application/json`,
                  s"""
                     |{
                     |  "requestId": "$RequestId",
                     |  "bundleId": "${BundleFrontend.bundleId}"
                     |}
                 """.stripMargin)
                )
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).unloadBundle(BundleFrontend.bundleId), timeout.duration) shouldBe BundleRequestSuccess(RequestId, BundleFrontend.bundleId)
      }
    }

    "handle error when unloading a bundle" in { f =>
      val sys = systemFixture(f)
      import sys._

      val errorMessage = s"No bundle found by the specified Bundle ID/name: '${BundleFrontend.bundleId}'"

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
          path(Segment) { bundleId =>
            delete {
              complete(StatusCodes.NotFound, errorMessage)
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).unloadBundle(BundleFrontend.bundleId), timeout.duration) shouldBe BundleRequestFailure(404, errorMessage)
      }
    }

    "stream events of all bundles" in { f =>
      val sys = systemFixture(f)
      import sys._

      val EventsCount = 10
      val expectedSse = ServerSentEvent("1234567890", "bundleExecutionAdded")

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles" / "events") {
          get {
            onComplete(Future.successful(Source.fromPublisher(ActorPublisher[ServerSentEvent](eventsPublisher(expectedSse))).take(EventsCount))) {
              case Success(source) => complete(source)
              case Failure(_) => complete(StatusCodes.TooManyRequests -> "Maximum number of bundle event publishers reached.")
            }
          }
        }
      // format: ON

      withServer(route) {
        val testSink = Flow[ServerSentEvent]
          .toMat(Sink.fold[Seq[ServerSentEvent], ServerSentEvent](Seq.empty[ServerSentEvent]) { case (result, sse) => result :+ sse })(Keep.right)
        Await.result(ControlClient(HostUrl).streamBundlesEvents(), timeout.duration) match {
          case EventStreamSuccess(source) =>
            val result = Await.result(source.runWith(testSink), 500.millis)
            result.size shouldBe EventsCount
            result shouldBe Seq.fill(EventsCount)(expectedSse)
          case EventStreamFailure(code, error) => fail(s"Streaming of events failed with http status code $code and error: $error")
        }
      }
    }

    "handle failure when streaming events of all bundles" in { f =>
      val sys = systemFixture(f)
      import sys._

      val errorMessage = "Maximum number of bundle event publishers reached."

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles" / "events") {
          get {
            complete {
              StatusCodes.TooManyRequests -> errorMessage
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).streamBundlesEvents(), timeout.duration) shouldBe EventStreamFailure(429, errorMessage)
      }
    }

    "retrieve events of a bundle" in { f =>
      val sys = systemFixture(f)
      import sys._

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
          path(Segment / "events") { bundleId =>
            get {
              parameter("count".as[Int].?) { count =>
                complete {
                  HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, BundleEventsAsJson))
                }
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).getBundleEvents(BundleFrontend.bundleId), timeout.duration) shouldBe BundleEventsSuccess(BundleEvents)
      }
    }

    "retrieve logs of a bundle" in { f =>
      val sys = systemFixture(f)
      import sys._

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
          path(Segment / "logs") { bundleId =>
            get {
              parameter("count".as[Int].?) { count =>
                complete {
                  HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, BundleLogsAsJson))
                }
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).getBundleLogs(BundleFrontend.bundleId), timeout.duration) shouldBe BundleLogsSuccess(BundleLogs)
      }
    }

    "retrieve information of all members" in { f =>
      val sys = systemFixture(f)
      import sys._

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "members") {
          get {
            complete {
              HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, MembersInfoAsJson))
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).getMembersInfo(), timeout.duration) shouldBe MembersInfo
      }
    }

    "retrieve information of one member" in { f =>
      val sys = systemFixture(f)
      import sys._

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "members") {
          path(Segment) { address =>
            get {
              complete {
                HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, MemberInfoAsJson))
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).getMemberInfo(MemberUpAddress), timeout.duration) shouldBe MemberInfo
      }
    }

    "handle error when retrieving information of one member" in { f =>
      val sys = systemFixture(f)
      import sys._

      val errorMessage = s"Unknown member: $MemberDownAddress"

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "members") {
          path(Segment) { address =>
            get {
              complete(StatusCodes.NotFound, errorMessage)
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).getMemberInfo(MemberDownAddress), timeout.duration) shouldBe MemberInfoFailure(404, errorMessage)
      }
    }

    "stream events of all members" in { f =>
      val sys = systemFixture(f)
      import sys._

      val EventsCount = 10
      val expectedSse = ServerSentEvent("1234567890", "bundleExecutionAdded")

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "members" / "events") {
          get {
            onComplete(Future.successful(Source.fromPublisher(ActorPublisher[ServerSentEvent](eventsPublisher(expectedSse))).take(EventsCount))) {
              case Success(source) => complete(source)
              case Failure(_) => complete(StatusCodes.TooManyRequests -> "Maximum number of bundle event publishers reached.")
            }
          }
        }
      // format: ON

      withServer(route) {
        val testSink = Flow[ServerSentEvent]
          .toMat(Sink.fold[Seq[ServerSentEvent], ServerSentEvent](Seq.empty[ServerSentEvent]) { case (result, sse) => result :+ sse })(Keep.right)
        Await.result(ControlClient(HostUrl).streamMembersEvents(), timeout.duration) match {
          case EventStreamSuccess(source) =>
            val result = Await.result(source.runWith(testSink), 500.millis)
            result.size shouldBe EventsCount
            result shouldBe Seq.fill(EventsCount)(expectedSse)
          case EventStreamFailure(code, error) => fail(s"Streaming of events failed with http status code $code and error: $error")
        }
      }
    }

    "handle failure when streaming events of all members" in { f =>
      val sys = systemFixture(f)
      import sys._

      val errorMessage = "Maximum number of bundle event publishers reached."

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "members" / "events") {
          get {
            complete {
              StatusCodes.TooManyRequests -> errorMessage
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).streamMembersEvents(), timeout.duration) shouldBe EventStreamFailure(429, errorMessage)
      }
    }

    "join a member" in { f =>
      val sys = systemFixture(f)
      import sys._

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "members") {
          post {
            extractRequest { request =>
              formField('joinTo) { joinTo =>
                complete {
                  StatusCodes.OK -> "OK"
                }
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).joinMember(MemberUpAddress), timeout.duration) shouldBe true
      }
    }

    "down a member" in { f =>
      val sys = systemFixture(f)
      import sys._

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "members") {
          put {
            formField('operation ! "down") {
              complete {
                StatusCodes.OK -> "OK"
              }
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).downMember(MemberUpAddress), timeout.duration) shouldBe true
      }
    }

    "leave a member" in { f =>
      val sys = systemFixture(f)
      import sys._

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "members") {
          delete {
            complete {
              StatusCodes.OK -> "OK"
            }
          }
        }
      // format: ON

      withServer(route) {
        Await.result(ControlClient(HostUrl).leaveMember(MemberUpAddress), timeout.duration) shouldBe true
      }
    }
  }

  "Bundle Events Payload" should {
    "be built correctly" in { f =>
      val fixture = systemFixture(f)
      import fixture._

      val controlClient = ControlClient(HostUrl)

      val httpPayload = controlClient.Payload.bundlesEvents(Set.empty)
      httpPayload.getRequestMethod shouldBe "GET"
      httpPayload.getUrl shouldBe new URL(s"${HostUrl}/v2/bundles/events")
    }

    "be built correctly when events are specified" in { f =>
      val fixture = systemFixture(f)
      import fixture._

      val controlClient = ControlClient(HostUrl)

      val httpPayload = controlClient.Payload.bundlesEvents(Set("bundleExecutionAdded", "bundleExecutionChanged"))
      httpPayload.getRequestMethod shouldBe "GET"
      httpPayload.getUrl shouldBe new URL(s"${HostUrl}/v2/bundles/events?events=bundleExecutionAdded&events=bundleExecutionChanged")
    }
  }

  def withServer(route: => Route)(testHandler: => Unit)(implicit system: ActorSystem, cc: ConnectionContext, HostUrl: URL, timeout: Timeout): Unit = {
    import system.dispatcher
    import cc.actorMaterializer

    val server = Http(system).bindAndHandle(route, HostUrl.getHost, HostUrl.getPort)
    Await.ready(server, timeout.duration)

    try {
      testHandler
    } finally {
      server.foreach(_.unbind())
    }
  }

  def eventsPublisher(entity: ServerSentEvent)(implicit system: ActorSystem) = actor(new ActorPublisher[ServerSentEvent] {
    import context.dispatcher
    private var n = 0
    context.system.scheduler.schedule(Duration.Zero, 10.millis, self, "tick")
    override def receive = {
      case "tick" if isActive && totalDemand > 0 =>
        n += 1
        if (Set(0, 1, 2).contains(n % 10)) onNext(entity)
    }
  })
}
