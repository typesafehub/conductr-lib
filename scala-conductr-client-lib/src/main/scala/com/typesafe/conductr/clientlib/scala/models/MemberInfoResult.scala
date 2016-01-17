package com.typesafe.conductr.clientlib.scala.models

import java.net.URI

/**
 * HTTP result for retrieving information of a ConductR cluster member
 */
sealed trait MemberInfoResult

/**
 * Represents a HTTP success result for retrieving information of a ConductR cluster member
 * @param member The cluster member
 * @param isUnreachableFrom A sequence of members this member is unreachable from
 * @param detectedUnreachable Information which members has detected this member as unreachable
 */
final case class MemberInfoSuccess(
  member: Member,
  isUnreachableFrom: Seq[UniqueAddress],
  detectedUnreachable: Seq[URI]) extends MemberInfoResult

/**
 * Represents a HTTP failure result for retrieving information of a ConductR cluster member
 * @param code The HTTP status code
 * @param error The error message
 */
final case class MemberInfoFailure(code: Int, error: String) extends HttpFailure with MemberInfoResult