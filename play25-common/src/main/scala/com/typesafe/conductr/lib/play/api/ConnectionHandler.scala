package com.typesafe.conductr.lib.play.api

import com.google.inject.Inject
import com.typesafe.conductr.lib.HttpPayload
import com.typesafe.conductr.lib.scala.{ AbstractConnectionContext, AbstractConnectionHandler }
import _root_.play.api.libs.ws._

import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.JavaConversions._

/**
 * When performing Play.WS connections, this is the connection context to use.
 */
class ConnectionContext @Inject() (val wsClient: WSClient, implicit val executionContext: ExecutionContext)
  extends AbstractConnectionContext

/**
 * INTERNAL API
 * Handles the Play.WS requests and responses
 */
class ConnectionHandler extends AbstractConnectionHandler {

  override protected type CC = ConnectionContext

  override def withConnectedRequest[T](
    payload: Option[HttpPayload]
  )(handler: (Int, Map[String, Option[String]]) => Option[T])(implicit cc: CC): Future[Option[T]] = {

    payload.fold[Future[Option[T]]](Future.successful(None)) { p =>
      val url = p.getUrl
      val urlStr = url.toString

      val requestHeaders = Seq(
        "User-Agent" -> UserAgent,
        "Host" -> url.getHost
      ) ++ p.getRequestHeaders.toSeq

      val request = cc.wsClient.url(urlStr)
        .withHeaders(requestHeaders: _*)
        .withMethod(p.getRequestMethod)
        .withFollowRedirects(follow = false)
      import cc.executionContext
      request.execute().map { response =>
        handler(
          response.status.intValue(),
          response.allHeaders.foldLeft(Map.empty[String, Option[String]]) {
            case (m, (header, values)) => m.updated(header, values.lastOption)
          }
        )
      }
    }
  }
}
