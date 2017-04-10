package com.typesafe.conductr.clientlib.scala.models

/**
 * HTTP result for retrieving bundle descriptor.
 */
sealed trait BundleGetDescriptorResult

/**
 * Represents a HTTP success result for retrieving bundle descriptor.
 *
 * @param bundleDescriptor the bundle descriptor of the requested bundle.
 */
final case class BundleGetDescriptorSuccess(bundleDescriptor: BundleDescriptor) extends BundleGetDescriptorResult

/**
 * Represents a HTTP failure result for retrieving bundle descriptor.
 *
 * @param code The HTTP status code
 * @param error The error message
 */
final case class BundleGetDescriptorFailure(code: Int, error: String) extends HttpFailure with BundleGetDescriptorResult
