package com.typesafe.conductr.bundlelib.play.api

import javax.inject.Singleton

import com.typesafe.conductr.bundlelib.scala.{ CacheLike, LocationCache }
import com.typesafe.conductr.lib.play.api.{ ConnectionHandler, ConnectionContext }
import play.api.inject.{ Binding, Module }
import play.api.{ Configuration, Environment }

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
