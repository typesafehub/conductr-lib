package com.typesafe.conductr.bundlelib.play

import com.typesafe.conductr.bundlelib.scala.AbstractStatusService

import play.api.libs.concurrent.Execution.Implicits
import play.libs.F

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
    import Implicits.defaultContext
    signalStarted().recover(handleSignalOrExit)
  }

  override def signalStarted()(implicit cc: CC): Future[Option[Unit]] =
    handler.withConnectedRequest(createSignalStartedPayload)(handleSignal)

  /** JAVA API */
  def signalStartedOrExitWithContext(cc: CC): F.Promise[F.Option[Unit]] = {
    import Implicits.defaultContext
    signalStartedOrExit()(cc).map(_.toF).toF
  }

  /** JAVA API */
  def signalStartedWithContext(cc: CC): F.Promise[F.Option[Unit]] = {
    import Implicits.defaultContext
    signalStarted()(cc).map(_.toF).toF
  }
}
