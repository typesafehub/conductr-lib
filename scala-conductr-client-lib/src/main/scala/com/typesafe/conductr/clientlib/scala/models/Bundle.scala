package com.typesafe.conductr.clientlib.scala.models

/**
 * The Bundle object represents a ConductR bundle at runtime.
 */
final case class Bundle(
  bundleId: BundleId,
  bundleDigest: Digest,
  configurationDigest: Option[Digest],
  attributes: BundleAttributes,
  bundleConfig: Option[BundleConfig],
  bundleScale: Option[BundleScale],
  bundleExecutions: Iterable[BundleExecution],
  bundleInstallations: Iterable[BundleInstallation],
  hasError: Boolean)