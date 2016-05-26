package com.typesafe.conductr.clientlib.scala.models

import java.util.Date

/**
 * All logs and events representation must implement this trait.
 */
sealed trait LogEntry

/**
 * Represents events to be sent to clients.
 */
final case class BundleEvent(
  timestamp: Date,
  event: String,
  description: String
) extends LogEntry

/**
 * Represents logs to be sent to the clients.
 */
final case class BundleLog(
  timestamp: Date,
  host: String,
  message: String
) extends LogEntry