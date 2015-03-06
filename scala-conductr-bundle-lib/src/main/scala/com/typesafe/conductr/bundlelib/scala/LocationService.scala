/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import java.net.URI

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object LocationService extends LocationService(new ConnectionHandler)

class LocationService(handler: ConnectionHandler) extends AbstractLocationService(handler) {

  override protected type CC = ConnectionContext

  override def lookup(serviceName: String)(implicit cc: CC): Future[Option[(URI, Option[FiniteDuration])]] =
    handler.withConnectedRequest(createLookupPayload(serviceName))(handleLookup)
}
