package com.typesafe.conductr.bundlelib.lagom.javadsl

import com.typesafe.conductr.bundlelib.akka.{ Env => AkkaEnv }
import com.typesafe.conductr.bundlelib.play.api.{ Env => PlayEnv }
import play.api._
import play.api.inject.guice.{ GuiceApplicationBuilder, GuiceApplicationLoader }

/**
 * Including this class into a Lagom project will automatically set Lagom's
 * configuration up from ConductR environment variables. Add the following
 * to your application.conf in order to include it:
 *
 *    play.application.loader = "com.typesafe.conductr.bundlelib.lagom.javadsl.ConductRApplicationLoader"
 */
class ConductRApplicationLoader extends ApplicationLoader {
  def load(context: ApplicationLoader.Context): Application = {
    val systemName = AkkaEnv.mkSystemName("application")
    val conductRConfig = Configuration(AkkaEnv.asConfig(systemName)) ++ Configuration(PlayEnv.asConfig(systemName))
    val newConfig = context.initialConfiguration ++ conductRConfig
    val newContext = context.copy(initialConfiguration = newConfig)
    val prodEnv = Environment.simple(mode = Mode.Prod)
    new GuiceApplicationLoader(GuiceApplicationBuilder(environment = prodEnv)).load(newContext)
  }
}
