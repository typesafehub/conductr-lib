package com.typesafe.conductr.clientlib.scala.models

/**
 * HTTP result for retrieving bundle events
 */
sealed trait BundleEventsResult

/**
 * Represents a HTTP success result for retrieving bundle events
 * @param events The bundle events
 */
final case class BundleEventsSuccess(events: Seq[BundleEvent]) extends BundleEventsResult

/**
 * Represents a HTTP failure result for retrieving bundle events
 * @param code The HTTP status code
 * @param error The error message
 */
final case class BundleEventsFailure(code: Int, error: String) extends HttpFailure with BundleEventsResult