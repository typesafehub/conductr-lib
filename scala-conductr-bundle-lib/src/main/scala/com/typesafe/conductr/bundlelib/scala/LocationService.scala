package com.typesafe.conductr.bundlelib.scala

import scala.concurrent.Future
import scala.language.reflectiveCalls

object LocationService extends LocationService(new ConnectionHandler)

class LocationService(handler: ConnectionHandler) extends AbstractLocationService(handler) {

  override protected type CC = ConnectionContext

  override def lookup(serviceName: String)(implicit cc: CC): Future[Option[String]] = {
    import cc.executionContext
    handler.withConnectedRequest(createLookupPayload(serviceName))(handleLookup).map(toUri)
  }

  override def lookup(serviceName: String, cache: CacheLike)(implicit cc: CC): Future[Option[String]] =
    cache.getOrElseUpdate(serviceName) {
      handler.withConnectedRequest(createLookupPayload(serviceName))(handleLookup)
    }
}
