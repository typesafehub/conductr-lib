package com.typesafe.conductr.clientlib.scala.models

import java.net.URI

// Instead of using the name 'BundleConfigEndpoint' we should create the case class 'Endpoint'
// inside the companion object 'BundleConfig' to access it with 'BundleConfig.Endpoint'.
// However, when using 'BundleConfig.Endpoint' the Json.format in 'JsonMarshalling' doesn't work anymore!
final case class BundleConfigEndpoint(bindProtocol: String, services: Set[URI])

final case class BundleConfig(endpoints: Map[String, BundleConfigEndpoint])