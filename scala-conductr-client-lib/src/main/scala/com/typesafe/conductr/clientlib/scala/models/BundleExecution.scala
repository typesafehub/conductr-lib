package com.typesafe.conductr.clientlib.scala.models

// Instead of using the name 'BundleExecutionEndpoint' we should create the case class 'Endpoint'
// inside the companion object 'BundleExecution' to access it with 'BundleExecution.Endpoint'.
// However, when using 'BundleExecution.Endpoint' the Json.format in 'JsonMarshalling' doesn't work anymore!
final case class BundleExecutionEndpoint(bindPort: Int, hostPort: Int)

/**
 * Represent a bundle execution
 */
final case class BundleExecution(
  host: Host,
  endpoints: Map[String, BundleExecutionEndpoint],
  isStarted: Boolean)