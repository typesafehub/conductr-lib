package com.typesafe.conductr.clientlib.scala.models

/**
 * Represents declaration of request mappings for a given endpoint.
 */
case class RequestAcl(protocolFamilyRequestMappings: Set[ProtocolFamilyRequestMappings])