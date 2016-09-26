package com.typesafe.conductr.lib.play

import com.ning.http.client.AsyncHttpClientConfig
import com.typesafe.conductr.lib.HttpPayload
import com.typesafe.conductr.lib.scala.{ AbstractConnectionHandler, AbstractConnectionContext }
import play.api.libs.ws.ning.{ NingWSClientConfig, NingWSClient, NingAsyncHttpClientConfigBuilder }
import play.api.libs.ws.WSClient
import play.api.libs.concurrent.Execution.{ Implicits => PlayImplicits }

import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.JavaConversions._

object ConnectionContext {
  def apply(executionContext: ExecutionContext): ConnectionContext =
    new ConnectionContext(Implicits.wsClient)(executionContext)

  /** JAVA API */
  def create(executionContext: ExecutionContext): ConnectionContext =
    apply(executionContext)

  object Implicits {
    /**
     * A WS client for the purposes of communicating with ConductR.
     */
    implicit val wsClient = {
      val ningClientConfig = new NingAsyncHttpClientConfigBuilder(new NingWSClientConfig()).build()
      val clientConfig = new AsyncHttpClientConfig.Builder(ningClientConfig)
        .setCompressionEnforced(true)
        .build()
      new NingWSClient(clientConfig)
    }

    /**
     * An implicit defaultContext ConnectionContext.
     * Import Implicits when you want to provide the global ConnectionContext implicitly.
     * This global ConnectionContext uses Play's default execution context.
     */
    implicit val defaultContext = ConnectionContext(PlayImplicits.defaultContext)
  }
}

/**
 * When performing Play.WS connections, this is the connection context to use.
 */
class ConnectionContext(val wsClient: WSClient)(implicit val executionContext: ExecutionContext)
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
