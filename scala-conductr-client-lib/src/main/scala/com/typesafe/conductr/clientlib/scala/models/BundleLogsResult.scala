package com.typesafe.conductr.clientlib.scala.models

/**
 * HTTP result for retrieving bundle log messages
 */
sealed trait BundleLogsResult

/**
 * Represents a HTTP success result for retrieving bundle log messages
 * @param logs The bundle log messages
 */
final case class BundleLogsSuccess(logs: Seq[BundleLog]) extends BundleLogsResult

/**
 * Represents a HTTP failure result for retrieving bundle log messages
 * @param code The HTTP status code
 * @param error The error message
 */
final case class BundleLogsFailure(code: Int, error: String) extends HttpFailure with BundleLogsResult
