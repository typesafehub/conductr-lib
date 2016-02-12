package com.typesafe.conductr.lib.scala

import com.typesafe.conductr.lib.HttpPayload
import scala.concurrent.Future

/**
 * A connection context supplies any context that is required in order for a connection handler to
 * perform its connection. For example, there is a ConnectionContext provides an ExecutionContext suitable
 * for performing blocking IO on.
 */
abstract class AbstractConnectionContext

/**
 * Connection handlers provide the means to establish a connection, issue a request and then finalize
 * the connection
 */
abstract class AbstractConnectionHandler {

  protected final val UserAgent = "TypesafeConductRBundleLib"

  protected type CC <: AbstractConnectionContext

  /**
   * Make a request to a ConductR service given a payload. Returns a future of an option. If there is some response
   * then a Some() will convey the result, otherwise None indicates that this program is not running in the context
   * of ConductR.
   */
  def withConnectedRequest[T](payload: Option[HttpPayload])(handler: (Int, Map[String, Option[String]]) => Option[T])(implicit cc: CC): Future[Option[T]]
}
