/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import java.io.IOException
import java.net.HttpURLConnection

import com.typesafe.conductr.bundlelib.HttpPayload

import scala.concurrent.{ blocking, Future, ExecutionContext }
import scala.util.{ Failure, Success, Try }

/**
 * Handles the JDK HttpURLConnection requests and responses
 */
object ConnectionHandler {
  private final val UserAgent = "TypesafeConductRBundleLib"

  /**
   * Make a request to a ConductR service given a payload. Returns a future of an option. If there is some response
   * then a Some() will convey the result, otherwise None indicates that this program is not running in the context
   * of ConductR.
   */
  def withConnectedRequest[T](
    payload: Option[HttpPayload])(thunk: HttpURLConnection => Option[T])(implicit ec: ExecutionContext): Future[Option[T]] =

    payload.fold[Future[Option[T]]](Future.successful(None)) { p =>
      Future {
        Try(p.getUrl.openConnection) match {
          case Success(con: HttpURLConnection) =>
            con.setRequestMethod(p.getRequestMethod)
            con.setInstanceFollowRedirects(p.getFollowRedirects)
            con.setRequestProperty("User-Agent", UserAgent)
            blocking {
              con.connect()
              try {
                thunk(con)
              } finally {
                con.disconnect()
              }
            }
          case Success(con) =>
            throw new IOException(s"Unexpected type of connection $con for $p")
          case Failure(e) =>
            throw new IOException(s"Connection failed for $p", e)
        }
      }
    }

}
