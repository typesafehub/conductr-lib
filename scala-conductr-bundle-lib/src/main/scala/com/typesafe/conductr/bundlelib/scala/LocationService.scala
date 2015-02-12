/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

import com.typesafe.conductr.bundlelib.{ HttpPayload, LocationService => JavaLocationService }
import com.typesafe.conductr.bundlelib.scala.ConnectionHandler.withConnectedRequest

import scala.concurrent._
import scala.concurrent.duration._

/**
 * LocationService used to look up services using the Typesafe ConductR Service Locator.
 */
object LocationService {

  private val MaxAgePattern = """.*max-age=(\d+).*""".r

  /**
   * Create the HttpPayload necessary to look up a service by name.
   *
   * If the service is available and can bee looked up the response for the HTTP request should be
   * 307 (Temporary Redirect), and the resulting URI to the service is in the "Location" header of the response.
   * A Cache-Control header may also be returned indicating the maxAge that the location should be cached for.
   * If the service can not be looked up the response should be 404 (Not Found).
   * All other response codes are considered illegal.
   *
   * @param serviceName The name of the service
   * @return Some HttpPayload describing how to do the service lookup or None if
   * this program is not running within ConductR
   */
  def createLookupPayload(serviceName: String): Option[HttpPayload] =
    Option(JavaLocationService.createLookupPayload(serviceName))

  /**
   * Look up a service by service name. Service names correspond to those declared in a Bundle
   * component's endpoint data structure i.e. within a bundle's bundle.conf.
   *
   * Returns some URI representing the service or None if the service is not found or if this
   * program is not running in the context of ConductR. An optional maxAge duration is also returned
   * indicating that the value may be cached for by the caller up to this period of time.
   */
  def lookup(serviceName: String)(implicit ec: ExecutionContext): Future[Option[(URI, Option[FiniteDuration])]] =
    withConnectedRequest(createLookupPayload(serviceName)) { con =>
      con.getResponseCode match {
        case 307 =>
          Option(con.getHeaderField("Location"))
            .map { location =>
              val uri = new URI(location)
              val maxAge = Option(con.getHeaderField("Cache-Control")).flatMap {
                case MaxAgePattern(maxAgeSecs) => Some(FiniteDuration(maxAgeSecs.toInt, TimeUnit.SECONDS))
                case _                         => None
              }
              uri -> maxAge
            }
            .orElse(throw new IOException("Missing Location header"))
        case 404 =>
          None
        case _ =>
          throw new IOException(s"Illegal response code ${con.getResponseCode}")
      }
    }

  /**
   * A convenience for extracting a uri from a lookup e.g.
   * {{{
   *   LocationService.lookup("/someservice").map(LocationService.toUri)
   * }}}
   */
  def toUri(service: Option[(URI, Option[FiniteDuration])]): Option[URI] =
    service.map(_._1)
}
