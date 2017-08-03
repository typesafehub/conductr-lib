package com.typesafe.conductr.bundlelib.lagom.scaladsl

import java.net.{ URI => JavaURI }

import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.client.{ CircuitBreakersPanel, CircuitBreakingServiceLocator }
import com.typesafe.conductr.bundlelib.play.api.LocationService
import com.typesafe.conductr.bundlelib.scala.{ CacheLike, URI }

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.reflectiveCalls

/**
 * ConductRServiceLocator implements Lagom's ServiceLocator by using the ConductR Service Locator.
 */
class ConductRServiceLocator(locationService: LocationService, cache: CacheLike, circuitBreakers: CircuitBreakersPanel)(implicit ec: ExecutionContext) extends CircuitBreakingServiceLocator(circuitBreakers) {

  override def locate(name: String, serviceCall: Descriptor.Call[_, _]): Future[Option[JavaURI]] =
    locationService.lookup(name, URI(""), cache)
}
