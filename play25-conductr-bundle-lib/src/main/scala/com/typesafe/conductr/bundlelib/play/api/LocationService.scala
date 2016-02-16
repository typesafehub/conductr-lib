package com.typesafe.conductr.bundlelib.play.api

import java.net.URI
import javax.inject.Inject

import com.typesafe.conductr.bundlelib.scala.{ AbstractLocationService, CacheLike }
import com.typesafe.conductr.lib.play.api.{ ConnectionContext, ConnectionHandler }

import scala.concurrent.Future
import scala.language.reflectiveCalls

/**
 * LocationService used to look up services using the Typesafe ConductR Service Locator.
 */
class LocationService @Inject() (handler: ConnectionHandler, val cc: LocationService#CC) extends AbstractLocationService(handler) {
  override protected type CC = ConnectionContext

  override def lookup(serviceName: String, fallback: URI, cache: CacheLike)(implicit cc: CC = this.cc): Future[Option[URI]] =
    if (Env.isRunByConductR)
      cache.getOrElseUpdate(serviceName) {
        handler.withConnectedRequest(createLookupPayload(serviceName))(handleLookup)
      }
    else
      Future.successful(Some(fallback))

}
