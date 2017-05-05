package com.typesafe.conductr.bundlelib.play

import com.typesafe.conductr.bundlelib.akka.{ Env => AkkaEnv }
import com.typesafe.conductr.bundlelib.play.{ Env => PlayEnv }
import play.api.inject.guice.{ GuiceApplicationBuilder, GuiceApplicationLoader }
import play.api._

/**
 * Including this class into a Play project will automatically set Play's
 * configuration up from ConductR environment variables. Add the following
 * to your application.conf in order to include it:
 *
 *    play.application.loader = "com.typesafe.conductr.bundlelib.play.ConductRApplicationLoader"
 */
class ConductRApplicationLoader extends ApplicationLoader {
  def load(context: ApplicationLoader.Context): Application = {
    val systemName = AkkaEnv.mkSystemName("application")
    val conductRConfig = Configuration(AkkaEnv.asConfig(systemName)) ++ Configuration(PlayEnv.asConfig(systemName))
    val newConfig = context.initialConfiguration ++ conductRConfig
    val newContext = context.copy(initialConfiguration = newConfig)
    val prodEnv = Environment.simple(mode = Mode.Prod)
    new GuiceApplicationLoader(new GuiceApplicationBuilder(environment = prodEnv)).load(newContext)
  }
}
