/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import java.io.IOException
import java.net.URI

import com.typesafe.conductr.bundlelib.{ LocationService => JavaLocationService, Env }
import com.typesafe.conductr.bundlelib.scala.ConnectionHandler.withConnectedRequest

import scala.concurrent._
import scala.concurrent.duration._

/**
 * LocationService used to look up services using the Typesafe ConductR Service Locator.
 */
object LocationService {

  /**
   * Look up a service by service name. Service names correspond to those declared in a Bundle
   * component's endpoint data structure i.e. within a bundle's bundle.conf.
   *
   * Returns some URI representing the service or None if the service is not found or if this
   * program is not running in the context of ConductR. An optional maxAge duration is also returned
   * indicating that the value may be cached for by the caller up to this period of time.
   */
  def lookup(serviceName: String)(implicit ec: ExecutionContext): Future[Option[(URI, Option[FiniteDuration])]] =
    withConnectedRequest(Option(JavaLocationService.createLookupPayload(serviceName))) { con =>
      con.getResponseCode match {
        case 307 =>
          Option(con.getHeaderField("Location"))
            .map { location =>
              val uri = new URI(location)
              uri -> None // FIXME: Need to interpret max-age here.
            }
            .orElse(throw new IOException("Missing Location header"))
        case 404 =>
          None
        case _ =>
          throw new IOException(s"Illegal response code ${con.getResponseCode}")
      }
    }

  /**
   * Return a service URI if there is one, stripping out any maxAge duration.
   * Otherwise either exit if running within ConductR, or default to another URI
   * if running outside of ConductR e.g. when in development mode.
   */
  def getUriOrExit(default: URI)(service: Option[(URI, Option[FiniteDuration])]): URI =
    service.map(_._1).getOrElse(exit(default))

  private def exit[T](default: T): T =
    Option(Env.BUNDLE_ID) match {
      case Some(_) =>
        System.exit(70)
        default
      case None =>
        default
    }
}
