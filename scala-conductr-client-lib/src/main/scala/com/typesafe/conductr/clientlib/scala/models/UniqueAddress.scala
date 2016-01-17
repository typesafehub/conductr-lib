package com.typesafe.conductr.clientlib.scala.models

import java.net.URI

/**
 * Member identifier consisting of address and random `uid`.
 * The `uid` is needed to be able to distinguish different
 * incarnations of a member with same hostname and port.
 */
case class UniqueAddress(address: URI, uid: Int)