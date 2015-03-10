/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.play

import com.typesafe.conductr.bundlelib.HttpPayload
import com.typesafe.conductr.bundlelib.scala.{ AbstractConnectionHandler, AbstractConnectionContext }
import play.{ Application => JavaApplication }
import play.api.{ Application, Play }
import play.api.libs.ws.WS
import play.api.libs.concurrent.Execution.{ Implicits => PlayImplicits }

import scala.concurrent.{ ExecutionContext, Future }

object ConnectionContext {
  def apply(executionContext: ExecutionContext, application: Application): ConnectionContext =
    new ConnectionContext(application)(executionContext)

  /** JAVA API */
  def create(executionContext: ExecutionContext, application: JavaApplication): ConnectionContext =
    apply(executionContext, application.getWrappedApplication)

  object Implicits {
    /**
     * An implicit defaultContext ConnectionContext.
     * Import global when you want to provide the global ConnectionContext implicitly.
     * This global ConnectionContext uses Play's default execution context.
     */
    implicit val defaultContext = ConnectionContext(PlayImplicits.defaultContext, Play.current)
  }
}

/**
 * When performing Play.WS connections, this is the connection context to use.
 */
class ConnectionContext(val application: Application)(implicit val executionContext: ExecutionContext) extends AbstractConnectionContext

/**
 * INTERNAL API
 * Handles the Play.WS requests and responses
 */
private[bundlelib] class ConnectionHandler extends AbstractConnectionHandler {

  override protected type CC = ConnectionContext

  override def withConnectedRequest[T](
    payload: Option[HttpPayload])(thunk: (Int, Map[String, Option[String]]) => Option[T])(implicit cc: CC): Future[Option[T]] = {

    payload.fold[Future[Option[T]]](Future.successful(None)) { p =>
      val url = p.getUrl
      val urlStr = url.toString

      val request = WS.url(urlStr)(cc.application)
        .withHeaders("User-Agent" -> UserAgent, "Host" -> url.getHost)
        .withMethod(p.getRequestMethod)
        .withFollowRedirects(follow = false)

      import cc.executionContext
      request.execute().map { response =>
        thunk(
          response.status.intValue(),
          response.allHeaders.foldLeft(Map.empty[String, Option[String]]) {
            case (m, (header, values)) => m.updated(header, values.lastOption)
          })
      }
    }
  }
}
