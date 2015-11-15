package com.typesafe.conductr.clientlib.scala.models

import scala.collection.immutable.SortedSet

/**
 * Describes a set of attributes that may accompany a bundle throughout the system.
 */
final case class BundleAttributes(
  system: String,
  nrOfCpus: Double,
  memory: Long,
  diskSpace: Long,
  roles: SortedSet[String],
  bundleName: String,
  systemVersion: String,
  compatibilityVersion: String)

