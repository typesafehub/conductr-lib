package com.typesafe.conductr.bundlelib.play

import java.util.Optional
import java.util.concurrent.CompletionStage

import com.typesafe.conductr.bundlelib.scala.AbstractStatusService
import com.typesafe.conductr.lib.play.{ ConnectionHandler, ConnectionContext }
import play.api.libs.concurrent.Execution.Implicits
import scala.compat.java8.OptionConverters._
import scala.compat.java8.FutureConverters._

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
  def signalStartedOrExitWithContext(cc: CC): CompletionStage[Optional[Unit]] = {
    import Implicits.defaultContext
    signalStartedOrExit()(cc).map(_.asJava).toJava
  }

  /** JAVA API */
  def signalStartedWithContext(cc: CC): CompletionStage[Optional[Unit]] = {
    import Implicits.defaultContext
    signalStarted()(cc).map(_.asJava).toJava
  }
}
