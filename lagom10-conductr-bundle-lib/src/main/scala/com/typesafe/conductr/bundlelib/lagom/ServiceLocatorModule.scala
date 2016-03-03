package com.typesafe.conductr.bundlelib.lagom

import javax.inject.Singleton

import com.lightbend.lagom.javadsl.api.ServiceLocator
import play.api.{ Configuration, Environment }
import play.api.inject.{ Binding, Module }

/**
 * This module binds the ServiceLocator interface from Lagom to the `ConductRServiceLocator`
 */
class ServiceLocatorModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    Seq(
      bind[ServiceLocator].to[ConductRServiceLocator].in[Singleton]
    )
}
