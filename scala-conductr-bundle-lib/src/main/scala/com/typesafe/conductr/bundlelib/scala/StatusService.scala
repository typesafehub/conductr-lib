package com.typesafe.conductr.bundlelib.scala

import com.typesafe.conductr.scala.{ ConnectionHandler, ConnectionContext }

import scala.concurrent.Future

object StatusService extends StatusService(new ConnectionHandler)

class StatusService(handler: ConnectionHandler) extends AbstractStatusService(handler) {

  override protected type CC = ConnectionContext

  override def signalStartedOrExit()(implicit cc: CC): Future[Option[Unit]] = {
    import cc.executionContext
    signalStarted().recover(handleSignalOrExit)
  }

  override def signalStarted()(implicit cc: CC): Future[Option[Unit]] =
    handler.withConnectedRequest(createSignalStartedPayload)(handleSignal)
}
