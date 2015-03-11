/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import scala.concurrent.Future
import scala.language.reflectiveCalls

object LocationService extends LocationService(new ConnectionHandler)

class LocationService(handler: ConnectionHandler) extends AbstractLocationService(handler) {

  override protected type CC = ConnectionContext

  override def lookup(serviceName: String)(implicit cc: CC): Future[Option[String]] = {
    import cc.executionContext
    handler.withConnectedRequest(createLookupPayload(serviceName))(handleLookup).map(toUri)
  }

  override def lookup(serviceName: String, cache: CacheLike)(implicit cc: CC): Future[Option[String]] =
    cache.getOrElseUpdate(serviceName) {
      handler.withConnectedRequest(createLookupPayload(serviceName))(handleLookup)
    }
}
