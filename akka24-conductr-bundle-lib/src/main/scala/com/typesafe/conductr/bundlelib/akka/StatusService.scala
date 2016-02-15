package com.typesafe.conductr.bundlelib.akka

import com.typesafe.conductr.lib.akka._
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

  override def signalStartedOrExit()(implicit cc: CC): Future[Option[Unit]] = {
    import Implicits.global
    signalStarted().recover(handleSignalOrExit)
  }

  override def signalStarted()(implicit cc: CC): Future[Option[Unit]] =
    handler.withConnectedRequest(createSignalStartedPayload)(handleSignal)

  /** JAVA API */
  def signalStartedOrExitWithContext(cc: CC): Future[JOption[Unit]] = {
    import Implicits.global
    signalStartedOrExit()(cc).map(JOption.fromScalaOption)
  }

  /** JAVA API */
  def signalStartedWithContext(cc: CC): Future[JOption[Unit]] = {
    import Implicits.global
    signalStarted()(cc).map(JOption.fromScalaOption)
  }
}
