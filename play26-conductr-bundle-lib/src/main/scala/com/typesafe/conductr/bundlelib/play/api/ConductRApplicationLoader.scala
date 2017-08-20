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
    val prodEnv = Environment.simple(mode = Mode.Prod)
    // The logic below is the default GuiceApplicationLoader behavior, but
    // with a patched environment and config. We can't use the default
    // GuiceApplicationLoader because there are binary incompatibilities
    // between Play 2.6.0 and 2.6.1. These incompatibilities make it difficult
    // to work directly with some methods of Application.Context. If we use
    // a GuiceApplicationBuilder then we can avoid using those methods on
    // the ApplicationLoader.Context.
    //
    // See: https://github.com/typesafehub/conductr-lib/issues/157
    GuiceApplicationBuilder(environment = prodEnv)
      .disableCircularProxies()
      .in(context.environment)
      .loadConfig(newConfig)
      .overrides(GuiceApplicationLoader.defaultOverrides(context): _*)
      .build()
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