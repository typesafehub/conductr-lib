package com.typesafe.conductr.lib.akka

import akka.actor._
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model._
import akka.http.scaladsl.{ Http, HttpExt }
import akka.http.scaladsl.model.headers.{ RawHeader, Host, `User-Agent` }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import com.typesafe.conductr.lib.HttpPayload
import com.typesafe.conductr.lib.scala.{ AbstractConnectionContext, AbstractConnectionHandler }

import scala.concurrent.Future
import scala.collection.JavaConversions._

object ConnectionContext {
  def apply()(implicit context: ActorRefFactory): ConnectionContext = {
    val system = actorSystemOf(context)
    apply(Http(system), ActorMaterializer.create(system))
  }

  def apply(httpExt: HttpExt, actorMaterializer: ActorMaterializer)(implicit context: ActorRefFactory): ConnectionContext =
    new ConnectionContext(httpExt, actorMaterializer, context)

  /** JAVA API */
  def create(context: ActorRefFactory): ConnectionContext =
    apply()(context)

  /** JAVA API */
  def create(httpExt: HttpExt, actorMaterializer: ActorMaterializer, context: ActorRefFactory): ConnectionContext =
    apply(httpExt, actorMaterializer)(context)

  private def actorSystemOf(context: ActorRefFactory): ActorSystem = {
    val system = context match {
      case s: ExtendedActorSystem => s
      case c: ActorContext        => c.system
      case null                   => throw new IllegalArgumentException("ActorRefFactory context must be defined")
      case _ =>
        throw new IllegalArgumentException(s"ActorRefFactory context must be a ActorSystem or ActorContext, got [${context.getClass.getName}]")
    }
    system
  }
}

class ConnectionContext(
  val httpExt: HttpExt,
  implicit val actorMaterializer: ActorMaterializer,
  implicit val context: ActorRefFactory
) extends AbstractConnectionContext

/**
 * Mix this trait into your Actor if you need an implicit
 * ConnectionContext in scope.
 *
 * Subclass may override `httpExt` and `actorMaterializer` to define custom
 * values for the `ConnectionContext`.
 */
trait ImplicitConnectionContext { this: Actor =>

  def httpExt: HttpExt =
    Http(context.system)

  def actorMaterializer: ActorMaterializer =
    ActorMaterializer.create(context)

  final implicit val cc: ConnectionContext = ConnectionContext(httpExt, actorMaterializer)
}

/**
 * INTERNAL API
 * Handles the Akka-http requests and responses
 */
class ConnectionHandler extends AbstractConnectionHandler {

  override protected type CC = ConnectionContext

  override def withConnectedRequest[T](payload: Option[HttpPayload])(handler: (Int, Map[String, Option[String]]) => Option[T])(implicit cc: CC): Future[Option[T]] = {
    payload.fold[Future[Option[T]]](Future.successful(None)) { p =>
      val connection = createConnection(p)
      val request = createRequest(p)

      Source.fromFuture(request)
        .via(connection)
        .map { response =>
          handler(
            response.status.intValue(),
            response.headers.foldLeft(Map.empty[String, Option[String]]) {
              case (m, header) => m.updated(header.name(), Some(header.value()))
            }
          )
        }
        .runWith(Sink.head)(cc.actorMaterializer)
    }
  }

  // TODO: Refactor this so that the body is part of `HttpPayload`. As a body type use [[org.reactivestreams.Publisher<T>]].
  def withConnectedRequest[T](payload: HttpPayload, body: Option[Future[RequestEntity]] = None)(handler: (Int, Map[String, Option[String]], ResponseEntity) => Future[T])(implicit cc: CC): Future[T] = {
    val connection = createConnection(payload)
    val request = createRequest(payload, body)

    Source.fromFuture(request)
      .via(connection)
      .mapAsync(1) { response =>
        handler(
          response.status.intValue(),
          response.headers.foldLeft(Map.empty[String, Option[String]]) {
            case (m, header) => m.updated(header.name(), Some(header.value()))
          },
          response.entity
        )
      }
      .runWith(Sink.head)(cc.actorMaterializer)
  }

  private def createConnection(payload: HttpPayload)(implicit cc: CC) =
    cc.httpExt.outgoingConnection(payload.getUrl.getHost, payload.getUrl.getPort)

  def createRequest(payload: HttpPayload, body: Option[Future[RequestEntity]] = None)(implicit cc: CC): Future[HttpRequest] = {
    val url = payload.getUrl

    val requestBuilder = payload.getRequestMethod match {
      case "GET"     => Get
      case "POST"    => Post
      case "PUT"     => Put
      case "PATCH"   => Patch
      case "DELETE"  => Delete
      case "OPTIONS" => Options
      case "HEAD"    => Head
    }

    import cc.actorMaterializer.executionContext
    val requestF = body
      .fold(Future.successful(requestBuilder(url.toString))) { _.map(b => requestBuilder(url.toString, b)) }

    requestF.map { request =>
      request.addHeader(`User-Agent`(UserAgent))
      request.addHeader(Host(url.getHost, url.getPort))
      payload.getRequestHeaders.foldLeft(request) { (request, entry) =>
        val (header, headerValue) = entry
        request.addHeader(RawHeader(header, headerValue))
      }
    }
  }
}
