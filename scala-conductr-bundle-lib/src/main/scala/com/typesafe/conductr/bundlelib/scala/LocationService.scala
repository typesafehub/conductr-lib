package com.typesafe.conductr.bundlelib.scala

import java.net.{ URI => JavaURI }

import com.typesafe.conductr.scala.{ ConnectionContext, ConnectionHandler }

import scala.concurrent.Future

object LocationService extends LocationService(new ConnectionHandler)

class LocationService(handler: ConnectionHandler) extends AbstractLocationService(handler) {

  override protected type CC = ConnectionContext

  override def lookup(serviceName: String, fallback: JavaURI, cache: CacheLike)(implicit cc: CC): Future[Option[JavaURI]] =
    if (Env.isRunByConductR)
      cache.getOrElseUpdate(serviceName) {
        handler.withConnectedRequest(createLookupPayload(serviceName))(handleLookup)
      }
    else
      Future.successful(Some(fallback))
}
