/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import java.io.IOException
import java.net.URL

import com.typesafe.conductr.bundlelib.{ LocationService => JavaLocationService }
import com.typesafe.conductr.bundlelib.scala.ConnectionHandler.withConnectedRequest

import scala.concurrent._

/**
 * LocationService used to look up services using the Typesafe ConductR Service Locator.
 */
object LocationService {

  /**
   * Look up a service by service name. Service names correspond to those declared in a Bundle
   * component's endpoint data structure i.e. within a bundle's bundle.conf.
   *
   * Returns some URL representing the service or None if the service is not found or if this
   * program is not running in the context of ConductR.
   */
  def lookup(serviceName: String)(implicit ec: ExecutionContext): Future[Option[URL]] =
    withConnectedRequest(Option(JavaLocationService.createLookupPayload(serviceName))) { con =>
      con.getResponseCode match {
        case 307 =>
          Option(con.getHeaderField("Location"))
            .map(new URL(_))
            .orElse(throw new IOException("Missing Location header"))
        case _ =>
          throw new IOException(s"Illegal response code ${con.getResponseCode}")
      }
    }

}
