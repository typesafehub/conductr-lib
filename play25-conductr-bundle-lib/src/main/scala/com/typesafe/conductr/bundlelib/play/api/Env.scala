package com.typesafe.conductr.bundlelib.play.api

import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.conductr.bundlelib.akka.{ Env => AkkaEnv }

import scala.collection.JavaConverters._

/**
 * Provides functions to set up the Play environment in accordance with what ConductR provides.
 */
object Env extends com.typesafe.conductr.bundlelib.scala.Env {

  /**
   * See [[asConfig()]]
   */
  def asConfig: Config =
    asConfig(AkkaEnv.mkSystemName("application"))

  /**
   * Provides various Play related properties.
   */
  def asConfig(systemName: String): Config =
    ConfigFactory.parseMap(Map("play.akka.actor-system" -> systemName).asJava)
}
