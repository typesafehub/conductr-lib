package com.typesafe.conductr.bundlelib.lagom.javadsl

import java.net.{ URI => JavaURI }
import java.util.Optional
import java.util.concurrent.CompletionStage
import javax.inject.Inject

import com.lightbend.lagom.javadsl.api.Descriptor
import com.lightbend.lagom.javadsl.client.{ CircuitBreakersPanel, CircuitBreakingServiceLocator }
import com.typesafe.conductr.bundlelib.play.api.LocationService
import com.typesafe.conductr.bundlelib.scala.{ CacheLike, URI }

import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._
import scala.concurrent.Future
import scala.language.reflectiveCalls

/**
 * ConductRServiceLocator implements Lagom's ServiceLocator by using the ConductR Service Locator.
 */
class ConductRServiceLocator @Inject() (locationService: LocationService, cache: CacheLike, circuitBreakersPanel: CircuitBreakersPanel) extends CircuitBreakingServiceLocator(circuitBreakersPanel) {

  import locationService.cc.executionContext

  private def locateAsScala(name: String): Future[Optional[JavaURI]] =
    locationService.lookup(name, URI(""), cache).map(_.asJava)

  override def locate(name: String, serviceCall: Descriptor.Call[_, _]): CompletionStage[Optional[JavaURI]] =
    locateAsScala(name).toJava
}
