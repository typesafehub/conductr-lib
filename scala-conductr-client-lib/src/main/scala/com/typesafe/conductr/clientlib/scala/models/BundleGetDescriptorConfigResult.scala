package com.typesafe.conductr.clientlib.scala.models

import com.typesafe.config.ConfigObject

/**
 * HTTP result for retrieving bundle descriptor config.
 */
sealed trait BundleGetDescriptorConfigResult

/**
 * Represents a HTTP success result for retrieving bundle descriptor config.
 *
 * @param config the bundle descriptor of the requested bundle in [[ConfigObject]] form.
 */
final case class BundleGetDescriptorConfigSuccess(config: ConfigObject) extends BundleGetDescriptorConfigResult

/**
 * Represents a HTTP failure result for retrieving bundle descriptor config.
 *
 * @param code The HTTP status code
 * @param error The error message
 */
final case class BundleGetDescriptorConfigFailure(code: Int, error: String) extends HttpFailure with BundleGetDescriptorConfigResult