package com.typesafe.conductr.clientlib.scala.models

import java.net.URI

sealed trait MembersInfoResult

final case class MembersInfoSuccess(
  selfNode: UniqueAddress,
  members: Seq[Member],
  unreachable: Seq[UnreachableMember]
) extends MembersInfoResult

final case class MembersInfoFailure(code: Int, error: String) extends HttpFailure with MembersInfoResult
