package com.typesafe.conductr.bundlelib.lagom.scaladsl

import akka.actor.ActorSystem
import com.lightbend.lagom.internal.client.{ CircuitBreakerConfig, CircuitBreakerMetricsProviderImpl, CircuitBreakers }
import com.lightbend.lagom.internal.spi.CircuitBreakerMetricsProvider
import com.lightbend.lagom.scaladsl.api.{ AdditionalConfiguration, ProvidesAdditionalConfiguration, ServiceLocator }
import com.lightbend.lagom.scaladsl.client.ConfigurationServiceLocator
import com.typesafe.conductr.bundlelib.akka.{ Env => AkkaEnv }
import com.typesafe.conductr.bundlelib.play.api.{ BundlelibComponents, ConductRLifecycleComponents, Env => PlayEnv }
import com.typesafe.conductr.bundlelib.scala.Env
import play.api._

/**
 * Mixing in this trait to your application cake in prod mode will ensure
 * your application uses the ConductR service locator when this bundle
 * has been invoked by ConductR (otherwise it will fallback to Lagom's
 * configuration based service locator). The trait will load any ConductR
 * specific configuration, and register into the application lifecycle to
 * to notify ConductR that it's started.
 *
 * It's important to ensure that your application, if it overrides Lagom's
 * additionalConfiguration, invokes the super implementation and appends
 * its own configuration to that, otherwise it may end up overriding the
 * ConductR provided configuration.
 */
trait ConductRApplicationComponents extends ConductRServiceLocatorComponents with ConductRLifecycleComponents with ProvidesAdditionalConfiguration {
  /**
   * The ConductR configuration.
   */
  lazy val systemName = AkkaEnv.mkSystemName("application")
  lazy val conductRConfiguration: Configuration = Configuration(AkkaEnv.asConfig(systemName)) ++ Configuration(PlayEnv.asConfig(systemName))

  override def additionalConfiguration: AdditionalConfiguration = super.additionalConfiguration ++ conductRConfiguration
}

/**
 * Provides the ConductR service locator.
 */
trait ConductRServiceLocatorComponents extends BundlelibComponents {
  def actorSystem: ActorSystem
  def configuration: Configuration

  lazy val circuitBreakerMetricsProvider: CircuitBreakerMetricsProvider = new CircuitBreakerMetricsProviderImpl(actorSystem)
  lazy val circuitBreakerConfig: CircuitBreakerConfig = new CircuitBreakerConfig(configuration)
  lazy val circuitBreakers: CircuitBreakers = new CircuitBreakers(actorSystem, circuitBreakerConfig, circuitBreakerMetricsProvider)

  lazy val serviceLocator: ServiceLocator =
    if (Env.isRunByConductR)
      new ConductRServiceLocator(conductRLocationSevice, conductRCacheLike, circuitBreakers)(executionContext)
    else
      new ConfigurationServiceLocator(configuration, circuitBreakers)(executionContext)
}

