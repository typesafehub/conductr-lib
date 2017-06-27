package com.typesafe.conductr.bundlelib.akka

import java.net.URI

import com.typesafe.conductr.lib.akka._
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

  override def lookup(serviceName: String, fallback: URI, cache: CacheLike)(implicit cc: CC): Future[Option[URI]] =
    if (Env.isRunByConductR)
      cache.getOrElseUpdate(serviceName) {
        handler.withConnectedRequest(createLookupPayload(serviceName))(handleLookup)
      }
    else
      Future.successful(Some(fallback))

  /** JAVA API */
  def lookupWithContext(serviceName: String, fallback: URI, cache: CacheLike, cc: CC): Future[JOption[URI]] = {
    import Implicits.global
    lookup(serviceName, fallback, cache)(cc).map(JOption.fromScalaOption)
  }
}
