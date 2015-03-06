/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.akka

import com.typesafe.conductr.bundlelib.scala.AbstractStatusService

import akka.japi.{ Option => JOption }

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

object StatusService extends StatusService(new ConnectionHandler) {
  /** JAVA API */
  def getInstance: StatusService =
    StatusService
}

/**
 * StatusService used to communicate the bundle status to the Typesafe ConductR Status Server.
 */
class StatusService(handler: ConnectionHandler) extends AbstractStatusService(handler) {

  override protected type CC = ConnectionContext

  override def signalStartedOrExit()(implicit cc: CC): Future[Option[Unit]] =
    signalStarted().recover(handleSignalOrExit)(Implicits.global)

  override def signalStarted()(implicit cc: CC): Future[Option[Unit]] =
    handler.withConnectedRequest(createSignalStartedPayload)(handleSignal)

  /** JAVA API */
  def signalStartedOrExitWithContext(cc: CC): Future[JOption[Unit]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    signalStartedOrExit()(cc).map(JOption.fromScalaOption)
  }

  /** JAVA API */
  def signalStartedWithContext(cc: CC): Future[JOption[Unit]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    signalStarted()(cc).map(JOption.fromScalaOption)
  }
}
