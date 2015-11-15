package com.typesafe.conductr.clientlib.scala.models

import java.net.URI

/**
 * Descriptor of a node's bundle installation including its associated optional configuration.
 * @param uniqueAddress the unique address within the cluster
 * @param bundleFile the path to the bundle, has to be a `URI`, because `Path` is not serializable
 * @param configurationFile the optional path to the bundle, has to be a `URI`, because `Path` is not serializable
 */
final case class BundleInstallation(
  uniqueAddress: UniqueAddress,
  bundleFile: URI,
  configurationFile: Option[URI])