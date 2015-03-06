/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.akka

import java.net.URI

import com.typesafe.conductr.bundlelib.scala.AbstractLocationService

import akka.japi.{ Option => JOption }

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object LocationService extends LocationService(new ConnectionHandler) {
  /** JAVA API */
  def getInstance: LocationService =
    LocationService
}

/**
 * LocationService used to look up services using the Typesafe ConductR Service Locator.
 */
class LocationService(handler: ConnectionHandler) extends AbstractLocationService(handler) {
  override protected type CC = ConnectionContext

  override def lookup(serviceName: String)(implicit cc: CC): Future[Option[(URI, Option[FiniteDuration])]] =
    handler.withConnectedRequest(createLookupPayload(serviceName))(handleLookup)

  /** JAVA API */
  def lookupWithContext(serviceName: String, cc: CC): Future[JOption[(URI, JOption[FiniteDuration])]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    lookup(serviceName)(cc).map {
      case Some((uri, maxAge)) => JOption.some((uri, JOption.fromScalaOption(maxAge)))
      case None                => JOption.none
    }
  }
}
