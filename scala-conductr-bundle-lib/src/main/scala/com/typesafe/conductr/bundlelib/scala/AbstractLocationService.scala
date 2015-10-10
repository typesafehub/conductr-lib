package com.typesafe.conductr.bundlelib.scala

import java.io.IOException
import java.net.{ URL => JavaURL, URI => JavaURI }
import java.util.concurrent.TimeUnit

import com.typesafe.conductr.HttpPayload
import com.typesafe.conductr.scala.{ AbstractConnectionHandler, AbstractConnectionContext }
import com.typesafe.conductr.bundlelib.{ LocationService => JavaLocationService }

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
   * If the service is available and can be looked up the response for the HTTP request should be
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
   * is running in the context of ConductR. If it is not then a fallback is returned.
   */
  def getLookupUrl(serviceName: String, fallback: JavaURL): JavaURL =
    JavaLocationService.getLookupUrl(serviceName, fallback)

  /**
   * Look up a service by service name using a cache. Service names correspond to those declared in a Bundle
   * component's endpoint data structure i.e. within a bundle's bundle.conf. If the bundle component
   * has not been started by ConductR then the fallback will be used.
   *
   * Returns some URI representing the service or None if the service is not found.
   */
  def lookup(serviceName: String, fallback: JavaURI, cache: CacheLike)(implicit cc: CC): Future[Option[JavaURI]]

  protected def toUri(service: Option[(JavaURI, Option[FiniteDuration])]): Option[JavaURI] =
    service.map(_._1)

  private val MaxAgePattern = """.*max-age=(\d+).*""".r

  protected def handleLookup(responseCode: Int, headers: Map[String, Option[String]]): Option[(JavaURI, Option[FiniteDuration])] =
    responseCode match {
      case 307 =>
        val locationAndMaxAge = for (Some(location) <- headers.get("Location")) yield {
          val maxAge = for (Some(MaxAgePattern(maxAgeSecs)) <- headers.get("Cache-Control")) yield FiniteDuration(maxAgeSecs.toInt, TimeUnit.SECONDS)
          URI(location) -> maxAge
        }
        locationAndMaxAge.orElse(throw new IOException("Missing Location header"))
      case 404 =>
        None
      case _ =>
        throw new IOException(s"Illegal response code $responseCode")
    }
}
