package com.typesafe.conductr.clientlib.scala.models

import java.net.URI

// Instead of using the name 'BundleConfigEndpoint' we should create the case class 'Endpoint'
// inside the companion object 'BundleConfig' to access it with 'BundleConfig.Endpoint'.
// However, when using 'BundleConfig.Endpoint' the Json.format in 'JsonMarshalling' doesn't work anymore!
/**
 * Represents configuration for a particular endpoint.
 *
 * @param bindProtocol the protocol which the endpoint binds to. Valid values are `http`, `tcp`, or `udp`.
 *                     Proxying only works for `http` and `tcp` based endpoints.
 * @param serviceName optional. If specified, the endpoint can be looked up through service locator through
 *                    [[serviceName]].
 * @param services deprecated. List of services for a given endpoint. Deprecated in lieu of declaration using
 *                 [[requestAcls]].
 * @param requestAcls One or more request mappings to access the declared endpoint.
 */
final case class BundleConfigEndpoint(
  bindProtocol: String,
  serviceName: Option[String],
  @deprecated("Deprecated in lieu of Request ACL endpoint declaration", since = "ConductR 2.0") services: Set[URI],
  requestAcls: Seq[RequestAcl]
)

/**
 * Represents endpoint configurations for a given [[Bundle]].
 * @param endpoints Endpoints configuration
 */
final case class BundleConfig(endpoints: Map[String, BundleConfigEndpoint])