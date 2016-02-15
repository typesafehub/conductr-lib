package com.typesafe.conductr.bundlelib.play

import java.net.URI
import java.util.Optional
import java.util.concurrent.CompletionStage
import com.typesafe.conductr.lib.play.{ ConnectionHandler, ConnectionContext }
import com.typesafe.conductr.bundlelib.scala.{ CacheLike, AbstractLocationService }
import play.api.libs.concurrent.Execution.Implicits
import scala.compat.java8.OptionConverters._
import scala.compat.java8.FutureConverters._

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
  def lookupWithContext(serviceName: String, fallback: URI, cache: CacheLike, cc: CC): CompletionStage[Optional[URI]] = {
    import Implicits.defaultContext
    lookup(serviceName, fallback, cache)(cc).map(_.asJava).toJava
  }

}
