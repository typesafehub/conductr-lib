package com.typesafe.conductr.bundlelib.lagom

import javax.inject.Singleton

import com.lightbend.lagom.javadsl.api.ServiceLocator
import play.api.{ Configuration, Environment, Mode }
import play.api.inject.{ Binding, Module }

/**
 * This module binds the ServiceLocator interface from Lagom to the `ConductRServiceLocator`
 * The `ConductRServiceLocator` is only bound if the application has been started in `Prod` mode.
 * In `Dev` mode the embedded service locator of Lagom is used.  
 */
class ServiceLocatorModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    if(environment.mode == Mode.Prod)
      Seq(bind[ServiceLocator].to[ConductRServiceLocator].in[Singleton])
    else 
      Seq.empty  
}
