package com.typesafe.conductr.clientlib.scala.models

import java.net.URI

case class UnreachableMember(
  node: UniqueAddress,
  observedBy: Seq[UniqueAddress]
)
