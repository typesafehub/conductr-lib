/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import java.io.IOException

import com.typesafe.conductr.bundlelib.{ StatusService => JavaStatusService }
import com.typesafe.conductr.bundlelib.scala.ConnectionHandler.withConnectedRequest

import scala.concurrent.{ ExecutionContext, Future }

/**
 * StatusService used to communicate the bundle status to the Typesafe ConductR Status Server.
 */
object StatusService {

  /**
   * Signal that the bundle has started or exit the JVM if it fails. If the bundle fails to communicate that
   * it has started it will eventually be killed by the ConductR.
   *
   * This will exit the JVM if it fails with exit code 70 (EXIT_SOFTWARE, Internal Software Error,
   * as defined in BSD sysexits.h).
   *
   * The returned future will complete successfully if the ConductR acknowledges the start signal.
   * A Future of None will be returned if this program is not running in the context of ConductR.
   */
  def signalStartedOrExit()(implicit ec: ExecutionContext): Future[Option[Unit]] =
    signalStarted().recover {
      case _: Throwable => Some(System.exit(70))
    }

  /**
   * Signal that the bundle has started or throw IOException if it fails. If the bundle fails to communicate that
   * it has started it will eventually be killed by the ConductR.
   *
   * The returned future will complete successfully if the ConductR acknowledges the start signal.
   * A Future of None will be returned if this program is not running in the context of ConductR.
   */
  def signalStarted()(implicit ec: ExecutionContext): Future[Option[Unit]] =
    withConnectedRequest(Option(JavaStatusService.createSignalStartedPayload())) { con =>
      val responseCode = con.getResponseCode
      if (responseCode < 200 || responseCode >= 300)
        throw new IOException("Illegal response code " + responseCode)
      Some(())
    }
}
