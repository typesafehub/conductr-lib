package com.typesafe.conductr.bundlelib.play.api

import javax.inject.{ Inject, Singleton }

import play.api.inject.{ Binding, Module }
import play.api.{ Configuration, Environment, Logger }

/**
 * Takes care of managing ConductR lifecycle events. In order to enable
 * ConductR lifecycle events for your application, add the following to
 * your application.conf:
 *
 *   play.modules.enabled += "com.typesafe.conductr.bundlelib.play.api.ConductRLifecycleModule"
 */
class ConductRLifecycleModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    Seq(
      bind(classOf[ConductRLifecycle]).toSelf.eagerly()
    )
}

/**
 * Responsible for signalling ConductR that the application has started.
 */
@Singleton
class ConductRLifecycle @Inject() (statusService: StatusService) {

  statusService
    .signalStartedOrExit()
    .foreach { _ =>
      if (Env.isRunByConductR) Logger.info("Signalled start to ConductR")
    }(statusService.cc.executionContext)
}

/**
 * Takes care of managing ConductR lifecycle events. In order to enable
 * ConductR lifecycle events for your compile time dependency injected
 * application, extend this trait.
 */
trait ConductRLifecycleComponents {
  def conductRStatusService: StatusService

  new ConductRLifecycle(conductRStatusService)
}