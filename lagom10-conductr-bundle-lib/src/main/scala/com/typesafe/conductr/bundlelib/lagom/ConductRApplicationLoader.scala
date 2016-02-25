package com.typesafe.conductr.bundlelib.lagom

import com.typesafe.conductr.bundlelib.akka.{ Env => AkkaEnv }
import com.typesafe.conductr.bundlelib.play.api.{ Env => PlayEnv }
import com.typesafe.conductr.bundlelib.lagom.{ Env => LagomEnv }
import play.api.inject.guice.{ GuiceApplicationBuilder, GuiceApplicationLoader }
import play.api.{ Application, ApplicationLoader, Configuration, Environment, Mode }

/**
 * Including this class into a Play project will automatically set Play's
 * configuration up from ConductR environment variables. Add the following
 * to your application.conf in order to include it:
 *
 *    play.application.loader = "com.typesafe.conductr.bundlelib.lagom.ConductRApplicationLoader"
 */
class ConductRApplicationLoader extends ApplicationLoader {
  def load(context: ApplicationLoader.Context): Application = {
    val conductRConfig = Configuration(AkkaEnv.asConfig) ++ Configuration(PlayEnv.asConfig) ++ Configuration(LagomEnv.asConfig)
    val newConfig = context.initialConfiguration ++ conductRConfig
    val newContext = context.copy(initialConfiguration = newConfig)
    val prodEnv = Environment.simple(mode = Mode.Prod)
    (new GuiceApplicationLoader(new GuiceApplicationBuilder(environment = prodEnv))).load(newContext)
  }
}
