package com.typesafe.conductr.clientlib.akka

import java.net.URL

import akka.actor.ActorDSL._
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives
import akka.contrib.http.{ Directives => ContribDirectives }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import com.typesafe.conductr.lib.IsolatingAkkaUnitTest
import com.typesafe.conductr.lib.akka.ConnectionContext
import com.typesafe.conductr.clientlib.akka.models.{ EventStreamFailure, EventStreamSuccess }
import com.typesafe.conductr.clientlib.scala.models._

import de.heikoseeberger.akkasse.{ EventStreamMarshalling, ServerSentEvent }
import scala.concurrent.duration._
import scala.concurrent.{ Future, Await }
import akka.http.scaladsl.model._
import scala.util.{ Random, Failure, Success }

class ControlClientSpec extends IsolatingAkkaUnitTest("ControlClientSpec", "akka.loglevel = INFO") {

  import TestData._
  import Directives._
  import ContribDirectives._
  import JsonMarshalling._
  import EventStreamMarshalling._

  val ApiVersion = "v2"

  def systemFixture(f: this.FixtureParam) = new {
    implicit val system = f.system
    implicit val cc = ConnectionContext()
    implicit val mat = cc.actorMaterializer
    implicit val ec = cc.actorMaterializer.executionContext
    implicit val timeout = f.timeout
    implicit val HostUrl = new URL(s"http://127.0.0.1:${Random.nextInt(9999) + 1000}")
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

    "load a valid bundle" in { f =>
      val sys = systemFixture(f)
      import sys._

      // format: OFF
      val route =
        pathPrefix(ApiVersion / "bundles") {
          post {
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
      // format: ON

      withServer(route) {
        val request = ControlClient(HostUrl).loadBundle(BundleUri, None)
        Await.result(request, timeout.duration) shouldBe BundleRequestSuccess(RequestId, BundleFrontend.bundleId)
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

  def withServer(route: => Route)(testHandler: => Unit)(implicit system: ActorSystem, cc: ConnectionContext, HostUrl: URL): Unit = {
    import system.dispatcher
    import cc.actorMaterializer

    val server = Http(system).bindAndHandle(route, HostUrl.getHost, HostUrl.getPort)

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
