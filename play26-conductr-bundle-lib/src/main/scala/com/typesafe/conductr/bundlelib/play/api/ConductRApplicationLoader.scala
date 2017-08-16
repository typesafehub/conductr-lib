package com.typesafe.conductr.bundlelib.play.api

import com.typesafe.conductr.bundlelib.akka.{ Env => AkkaEnv }
import com.typesafe.conductr.bundlelib.play.api.{ Env => PlayEnv }
import play.api.inject.guice.{ GuiceApplicationBuilder, GuiceApplicationLoader }
import play.api.{ Application, ApplicationLoader, Configuration, Environment, Mode }

/**
 * Including this class into a Play project will automatically set Play's
 * configuration up from ConductR environment variables. Add the following
 * to your application.conf in order to include it:
 *
 *    play.application.loader = "com.typesafe.conductr.bundlelib.play.api.ConductRApplicationLoader"
 */
class ConductRApplicationLoader extends ApplicationLoader {
  def load(context: ApplicationLoader.Context): Application = {
    val systemName = AkkaEnv.mkSystemName("application")
    val conductRConfig = Configuration(AkkaEnv.asConfig(systemName)) ++ Configuration(PlayEnv.asConfig(systemName))
    val newConfig = context.initialConfiguration ++ conductRConfig
    val newContext =
      ApplicationLoader.Context(
        environment = context.environment,
        sourceMapper = context.sourceMapper,
        webCommands = context.webCommands,
        initialConfiguration = newConfig,
        lifecycle = context.lifecycle
      ) // Avoid copy to work-around bin incompatibility for Play: https://github.com/typesafehub/conductr-lib/issues/157
    val prodEnv = Environment.simple(mode = Mode.Prod)
    new GuiceApplicationLoader(GuiceApplicationBuilder(environment = prodEnv)).load(newContext)
  }
}

/**
 * Mixing this trait into your application cake will provide you with the
 * necessary configuration for Play from ConductR's environment variables.
 * You will need to mix this configuration into your own applications
 * configuration, like so:
 *
 * ```
 * class MyApplication(ctx: Context) extends BuiltInComponentsFromContext(ctx)
 *   with ConductRApplicationComponents
 *   with ... {
 *
 *   override lazy val configuration = super.configuration ++ conductRConfiguration
 *
 *   ...
 * }
 * ```
 *
 * It will also ensure provide the core ConductR bundle lib components, and
 * hook ConductR into the Play lifecycle.
 */
trait ConductRApplicationComponents extends BundlelibComponents with ConductRLifecycleComponents {

  lazy val systemName = AkkaEnv.mkSystemName("application")
  lazy val conductRConfiguration = Configuration(AkkaEnv.asConfig(systemName)) ++ Configuration(PlayEnv.asConfig)
}