package com.typesafe.conductr.bundlelib.play.api

import javax.inject.Inject

import com.typesafe.conductr.bundlelib.scala.AbstractStatusService
import com.typesafe.conductr.lib.play.api.{ ConnectionContext, ConnectionHandler }
import play.api.libs.concurrent.Execution.Implicits

import scala.concurrent.Future

/**
 * StatusService used to communicate the bundle status to the Typesafe ConductR Status Server.
 */
class StatusService @Inject() (handler: ConnectionHandler, val cc: StatusService#CC) extends AbstractStatusService(handler) {

  override protected type CC = ConnectionContext

  override def signalStartedOrExit()(implicit cc: CC = this.cc): Future[Option[Unit]] = {
    import Implicits.defaultContext
    signalStarted().recover(handleSignalOrExit)
  }

  override def signalStarted()(implicit cc: CC = this.cc): Future[Option[Unit]] =
    handler.withConnectedRequest(createSignalStartedPayload)(handleSignal)
}
