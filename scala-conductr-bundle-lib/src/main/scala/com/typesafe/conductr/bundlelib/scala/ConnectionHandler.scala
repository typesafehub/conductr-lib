/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import java.io.IOException
import java.net.HttpURLConnection

import com.typesafe.conductr.bundlelib.HttpPayload

import scala.concurrent._
import scala.util.{ Failure, Success, Try }

object ConnectionContext {
  def apply(executionContext: ExecutionContext): ConnectionContext =
    new ConnectionContext()(executionContext)

  object Implicits {
    /**
     * An implicit global ConnectionContext.
     * Import global when you want to provide the global ConnectionContext implicitly.
     * This global ConnectionContext uses Scala's global execution context.
     */
    implicit val global = ConnectionContext(ExecutionContext.Implicits.global)
  }
}

/**
 * When performing pure Scala connections, this is the connection context to use. Pass in the
 * execution context to be used for blocking IO.
 */
class ConnectionContext()(implicit val executionContext: ExecutionContext) extends AbstractConnectionContext

/**
 * Handles the JDK HttpURLConnection requests and responses
 */
private[bundlelib] class ConnectionHandler extends AbstractConnectionHandler {

  override protected type CC = ConnectionContext

  /**
   * Make a request to a ConductR service given a payload. Returns a future of an option. If there is some response
   * then a Some() will convey the result, otherwise None indicates that this program is not running in the context
   * of ConductR.
   */
  override def withConnectedRequest[T](
    payload: Option[HttpPayload])(thunk: (Int, Map[String, Option[String]]) => Option[T])(implicit cc: CC): Future[Option[T]] = {

    import cc.executionContext
    payload.fold[Future[Option[T]]](Future.successful(None)) { p =>
      Future {
        Try(p.getUrl.openConnection) match {
          case Success(connection: HttpURLConnection) =>
            connection.setRequestMethod(p.getRequestMethod)
            connection.setInstanceFollowRedirects(p.getFollowRedirects)
            connection.setRequestProperty("User-Agent", UserAgent)
            blocking {
              connection.connect()
              import scala.collection.JavaConverters._
              try {
                thunk(
                  connection.getResponseCode,
                  connection.getHeaderFields.asScala.foldLeft(Map.empty[String, Option[String]]) {
                    case (m, (k, v)) => m.updated(k, v.asScala.lastOption)
                  })

              } finally {
                connection.disconnect()
              }
            }
          case Success(connection) =>
            throw new IOException(s"Unexpected type of connection $connection for $p")
          case Failure(e) =>
            throw new IOException(s"Connection failed for $p", e)
        }
      }
    }
  }

}
