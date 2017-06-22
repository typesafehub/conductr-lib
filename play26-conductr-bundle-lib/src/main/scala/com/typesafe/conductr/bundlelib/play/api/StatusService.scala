package com.typesafe.conductr.bundlelib.play.api

import javax.inject.Inject

import com.typesafe.conductr.bundlelib.scala.AbstractStatusService
import com.typesafe.conductr.lib.play.api.{ ConnectionContext, ConnectionHandler }

import scala.concurrent.{ ExecutionContext, Future }

/**
 * StatusService used to communicate the bundle status to the Typesafe ConductR Status Server.
 */
class StatusService @Inject() (handler: ConnectionHandler, val cc: StatusService#CC, ec: ExecutionContext) extends AbstractStatusService(handler) {

  override protected type CC = ConnectionContext

  override def signalStartedOrExit()(implicit cc: CC = this.cc): Future[Option[Unit]] = {
    signalStarted().recover(handleSignalOrExit)(ec)
  }

  override def signalStarted()(implicit cc: CC = this.cc): Future[Option[Unit]] =
    handler.withConnectedRequest(createSignalStartedPayload)(handleSignal)
}
