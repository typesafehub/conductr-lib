package com.typesafe.conductr.bundlelib.lagom.javadsl

import javax.inject.Singleton

import com.lightbend.lagom.javadsl.api.ServiceLocator
import com.lightbend.lagom.javadsl.client.ConfigurationServiceLocator
import com.typesafe.conductr.bundlelib.scala.Env
import play.api.inject.{ Binding, Module }
import play.api.{ Configuration, Environment, Mode }

/**
 * This module binds the ServiceLocator interface from Lagom to the `ConductRServiceLocator` when
 * running from ConductR. If not running from ConductR then Lagom's configuration service
 * locator is used.
 * The service locators are only bound if the application has been started in `Prod` mode.
 * In `Dev` mode the embedded service locator of Lagom is used.
 */
class ServiceLocatorModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    if (environment.mode == Mode.Prod)
      if (Env.isRunByConductR)
        Seq(bind[ServiceLocator].to[ConductRServiceLocator].in[Singleton])
      else
        Seq(bind[ServiceLocator].to[ConfigurationServiceLocator].in[Singleton])
    else
      Seq.empty
}
