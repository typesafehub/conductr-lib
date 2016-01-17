package com.typesafe.conductr.clientlib.scala.models

/**
 * Contains the scaling information of a bundle.
 * @param scale The number of instances of the bundle to start.
 * @param affinity Optional: Identifier to other bundle.
 *                 If specified, the current bundle will be run on the same host
 *                 where the specified bundle is currently running.
 */
final case class BundleScale(scale: Int, affinity: Option[BundleId])