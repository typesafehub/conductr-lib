package com.typesafe.conductr.bundlelib.play.api

import javax.inject.Singleton

import com.typesafe.conductr.bundlelib.scala.{ CacheLike, LocationCache }
import com.typesafe.conductr.lib.play.api.{ ConnectionContext, ConnectionHandler }
import play.api.inject.{ Binding, Module }
import play.api.libs.ws.WSClient
import play.api.{ Configuration, Environment }

import scala.concurrent.ExecutionContext

/**
 * Provides all the core components of this bundle library. Add the following
 * to your application.conf in order to include it:
 *
 *   play.modules.enabled += "com.typesafe.conductr.bundlelib.play.api.BundlelibModule"
 */
class BundlelibModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    Seq(
      bind[ConnectionContext].toSelf.in[Singleton],
      bind[ConnectionHandler].toSelf.in[Singleton],
      bind[CacheLike].to[LocationCache].in[Singleton],
      bind[LocationService].toSelf.in[Singleton],
      bind[StatusService].toSelf.in[Singleton]
    )
}

/**
 * Provides all the core components of this bundle library to compile time dependency injected applications.
 */
trait BundlelibComponents {
  def wsClient: WSClient
  def executionContext: ExecutionContext

  lazy val conductRConnectionContext: ConnectionContext = new ConnectionContext(wsClient, executionContext)
  lazy val conductRConnectionHandler: ConnectionHandler = new ConnectionHandler
  lazy val conductRCacheLike: CacheLike = new LocationCache
  lazy val conductRLocationSevice: LocationService = new LocationService(conductRConnectionHandler, conductRConnectionContext)
  lazy val conductRStatusService: StatusService = new StatusService(conductRConnectionHandler, conductRConnectionContext)
}
