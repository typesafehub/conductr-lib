/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

import com.typesafe.conductr.bundlelib.{ HttpPayload, LocationService => JavaLocationService }

import scala.concurrent._
import scala.concurrent.duration._

/**
 * A Location Service is used to look up services using the Typesafe ConductR Service Locator.
 */
abstract class AbstractLocationService(handler: AbstractConnectionHandler) {

  protected type CC <: AbstractConnectionContext

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
   * A convenience function for [[createLookupPayload]] where the payload url is created when this bundle component
   * is running in the context of ConductR. If it is not then a fallbackUrl is returned.
   */
  def getLookupUrl(serviceName: String, fallbackUrl: String): String =
    JavaLocationService.getLookupUrl(serviceName, fallbackUrl)

  /**
   * Look up a service by service name. Service names correspond to those declared in a Bundle
   * component's endpoint data structure i.e. within a bundle's bundle.conf.
   *
   * Returns some URI representing the service or None if the service is not found or if this
   * program is not running in the context of ConductR. An optional maxAge duration is also returned
   * indicating that the value may be cached for by the caller up to this period of time.
   */
  def lookup(serviceName: String)(implicit cc: CC): Future[Option[(URI, Option[FiniteDuration])]]

  /**
   * A convenience for extracting a uri from a lookup e.g.
   * {{{
   *   LocationService.lookup("/someservice").map(LocationService.toUri)
   * }}}
   */
  def toUri(service: Option[(URI, Option[FiniteDuration])]): Option[URI] =
    service.map(_._1)

  private val MaxAgePattern = """.*max-age=(\d+).*""".r

  protected def handleLookup(responseCode: Int, headers: Map[String, Option[String]]): Option[(URI, Option[FiniteDuration])] =
    responseCode match {
      case 307 =>
        val location = for (Some(location) <- headers.get("Location")) yield {
          val uri = new URI(location)
          val maxAge = for (Some(MaxAgePattern(maxAgeSecs)) <- headers.get("Cache-Control")) yield FiniteDuration(maxAgeSecs.toInt, TimeUnit.SECONDS)
          uri -> maxAge
        }
        location.orElse(throw new IOException("Missing Location header"))
      case 404 =>
        None
      case _ =>
        throw new IOException(s"Illegal response code $responseCode")
    }
}
