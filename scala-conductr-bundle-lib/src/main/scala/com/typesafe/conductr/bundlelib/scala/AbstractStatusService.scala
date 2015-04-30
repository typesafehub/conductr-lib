package com.typesafe.conductr.bundlelib.scala

import java.io.IOException

import com.typesafe.conductr.bundlelib.{ StatusService => JavaStatusService, HttpPayload }

import scala.concurrent.Future

/**
 * StatusService used to communicate the bundle status to the Typesafe ConductR Status Server.
 */
abstract class AbstractStatusService(handler: AbstractConnectionHandler) {

  protected type CC <: AbstractConnectionContext

  /**
   * Create the HttpPayload necessary to signal that a bundle has started.
   *
   * Any 2xx response code is considered a success. Any other response code is considered a failure.
   *
   * @return Some HttpPayload describing how to signal that a bundle has started or None if
   *         this program is not running within ConductR
   */
  def createSignalStartedPayload: Option[HttpPayload] =
    Option(JavaStatusService.createSignalStartedPayload)

  /**
   * Signal that the bundle has started or exit the JVM if it fails.
   *
   * This will exit the JVM if it fails with exit code 70 (EXIT_SOFTWARE, Internal Software Error,
   * as defined in BSD sysexits.h).
   *
   * The returned future will complete successfully if the ConductR acknowledges the start signal.
   * A Future of None will be returned if this program is not running in the context of ConductR.
   */
  def signalStartedOrExit()(implicit cc: CC): Future[Option[Unit]]

  /**
   * Signal that the bundle has started or throw IOException if it fails. If the bundle fails to communicate that
   * it has started it will eventually be killed by the ConductR.
   *
   * The returned future will complete successfully if the ConductR acknowledges the start signal.
   * A Future of None will be returned if this program is not running in the context of ConductR.
   */
  def signalStarted()(implicit cc: CC): Future[Option[Unit]]

  protected def handleSignalOrExit: PartialFunction[Throwable, Option[Unit]] = {
    case _: Throwable => Some(System.exit(70))
  }

  protected def handleSignal(responseCode: Int, headers: Map[String, Option[String]]): Option[Unit] = {
    if (responseCode < 200 || responseCode >= 300)
      throw new IOException("Illegal response code " + responseCode)
    Some(())
  }
}
