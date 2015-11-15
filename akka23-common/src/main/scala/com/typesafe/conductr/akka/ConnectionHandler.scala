package com.typesafe.conductr.akka

import akka.actor._
import akka.http.scaladsl.client.RequestBuilding.{ Get, Post, Put, Patch, Delete, Options, Head }
import akka.http.scaladsl.{ Http, HttpExt }
import akka.http.scaladsl.model.headers.{ Host, `User-Agent` }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import com.typesafe.conductr.HttpPayload
import com.typesafe.conductr.scala.{ AbstractConnectionContext, AbstractConnectionHandler }

import scala.concurrent.Future

object ConnectionContext {
  def apply()(implicit context: ActorRefFactory): ConnectionContext = {
    val system = actorSystemOf(context)
    apply(Http(system), ActorMaterializer.create(system))
  }

  def apply(httpExt: HttpExt, actorMaterializer: ActorMaterializer): ConnectionContext =
    new ConnectionContext(httpExt, actorMaterializer)

  /** JAVA API */
  def create(context: ActorRefFactory): ConnectionContext =
    apply()(context)

  /** JAVA API */
  def create(httpExt: HttpExt, actorMaterializer: ActorMaterializer): ConnectionContext =
    apply(httpExt, actorMaterializer)

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
  implicit val actorMaterializer: ActorMaterializer) extends AbstractConnectionContext

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

  override def withConnectedRequest[T](
    payload: Option[HttpPayload])(thunk: (Int, Map[String, Option[String]]) => Option[T])(implicit cc: CC): Future[Option[T]] = {

    payload.fold[Future[Option[T]]](Future.successful(None)) { p =>
      val connection = cc.httpExt.outgoingConnection(p.getUrl.getHost, p.getUrl.getPort)
      val url = p.getUrl
      val urlStr = url.toString

      val requestMethod = p.getRequestMethod match {
        case "GET"     => Get(urlStr)
        case "POST"    => Post(urlStr)
        case "PUT"     => Put(urlStr)
        case "PATCH"   => Patch(urlStr)
        case "DELETE"  => Delete(urlStr)
        case "OPTIONS" => Options(urlStr)
        case "HEAD"    => Head(urlStr)
      }

      val request = requestMethod
        .addHeader(`User-Agent`(UserAgent))
        .addHeader(Host(url.getHost))

      val requestSource = Source.single(request)
        .via(connection)
        .map { response =>
          thunk(
            response.status.intValue(),
            response.headers.foldLeft(Map.empty[String, Option[String]]) {
              case (m, header) => m.updated(header.name(), Some(header.value()))
            })
        }
      requestSource.runWith(Sink.head)(cc.actorMaterializer)
    }

  }
}
