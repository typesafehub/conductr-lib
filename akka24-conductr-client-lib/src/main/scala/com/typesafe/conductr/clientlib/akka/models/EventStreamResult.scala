package com.typesafe.conductr.clientlib.akka.models

import akka.stream.scaladsl.Source
import com.typesafe.conductr.clientlib.scala.models.HttpFailure
import de.heikoseeberger.akkasse.ServerSentEvent

sealed trait EventStreamResult

final case class EventStreamSuccess(source: Source[ServerSentEvent, Unit]) extends EventStreamResult

final case class EventStreamFailure(code: Int, error: String) extends HttpFailure with EventStreamResult
