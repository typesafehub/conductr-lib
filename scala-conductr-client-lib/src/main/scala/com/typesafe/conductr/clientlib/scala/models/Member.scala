package com.typesafe.conductr.clientlib.scala.models

/**
 * Describes a ConductR cluster member
 */
final case class Member(
  node: UniqueAddress,
  status: String,
  roles: Set[String]
)