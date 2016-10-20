package com.typesafe.conductr.clientlib.scala.models

import org.reactivestreams.Publisher

/**
 * HTTP result for retrieving bundle.
 */
sealed trait BundleGetResult

/**
 * Represents the bundle file obtained from ConductR
 * @param fileName the name of the file
 * @param data the stream of data bytes of the file
 */
final case class BundleFile(fileName: String, data: Publisher[Array[Byte]])

/**
 * Represents the bundle configuration file obtained from ConductR
 * @param fileName the name of the file
 * @param data the stream of data bytes of the file
 */
final case class BundleConfigurationFile(fileName: String, data: Publisher[Array[Byte]])

/**
 * Represents a HTTP success result for retrieving bundle.
 *
 * @param bundleId the given bundle id of the requested bundle.
 */
final case class BundleGetSuccess(bundleId: BundleId, bundleFileName: String, configFileName: Option[String]) extends BundleGetResult

/**
 * Represents a HTTP failure result for retrieving bundle.
 *
 * @param code The HTTP status code
 * @param error The error message
 */
final case class BundleGetFailure(code: Int, error: String) extends HttpFailure with BundleGetResult

/**
 * Thrown when HTTP response is invalid, i.e. doesn't contain any bundle file.
 *
 * @param message The error message
 */
final case class InvalidBundleGetResponseBody(message: String) extends RuntimeException(message)