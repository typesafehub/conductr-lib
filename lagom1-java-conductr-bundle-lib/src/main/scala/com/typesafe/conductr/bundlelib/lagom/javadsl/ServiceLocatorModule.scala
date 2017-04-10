package com.typesafe.conductr.bundlelib.lagom.javadsl

import javax.inject.Singleton

import com.lightbend.lagom.javadsl.api.ServiceLocator
import com.typesafe.conductr.bundlelib.scala.Env
import play.api.inject.{ Binding, Module }
import play.api.{ Configuration, Environment }

/**
 * This module binds the ServiceLocator interface from Lagom to the `ConductRServiceLocator`
 * The `ConductRServiceLocator` is only bound if the application has been started in `Prod` mode.
 * In `Dev` mode the embedded service locator of Lagom is used.
 */
class ServiceLocatorModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    if (Env.isRunByConductR)
      Seq(bind[ServiceLocator].to[ConductRServiceLocator].in[Singleton])
    else
      Seq.empty
}
