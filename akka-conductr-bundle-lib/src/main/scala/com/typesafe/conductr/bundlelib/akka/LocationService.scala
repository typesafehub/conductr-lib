/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.akka

import com.typesafe.conductr.bundlelib.scala.{ CacheLike, AbstractLocationService }

import akka.japi.{ Option => JOption }

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future
import scala.language.reflectiveCalls

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

  override def lookup(serviceName: String)(implicit cc: CC): Future[Option[String]] = {
    import Implicits.global
    handler.withConnectedRequest(createLookupPayload(serviceName))(handleLookup).map(toUri)
  }

  override def lookup(serviceName: String, cache: CacheLike)(implicit cc: CC): Future[Option[String]] =
    cache.getOrElseUpdate(serviceName) {
      handler.withConnectedRequest(createLookupPayload(serviceName))(handleLookup)
    }

  /** JAVA API */
  def lookupWithContext(serviceName: String, cc: CC, cache: CacheLike): Future[JOption[String]] = {
    import Implicits.global
    lookup(serviceName, cache)(cc).map(JOption.fromScalaOption)
  }
}
