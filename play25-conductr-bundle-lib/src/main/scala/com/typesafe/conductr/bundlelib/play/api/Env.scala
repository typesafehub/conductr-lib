package com.typesafe.conductr.bundlelib.play.api

import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.conductr.bundlelib.akka.{ Env => AkkaEnv }

import scala.collection.JavaConverters._

/**
 * Provides functions to set up the Play environment in accordance with what ConductR provides.
 */
object Env extends com.typesafe.conductr.bundlelib.scala.Env {

  /**
   * Provides various Play related properties.
   */
  def asConfig: Config =
    ConfigFactory.parseMap(playActorSystem.toMap.asJava)

  private def playActorSystem: List[(String, String)] =
    (for {
      bundleSystem <- sys.env.get("BUNDLE_SYSTEM")
      bundleSystemVersion <- sys.env.get("BUNDLE_SYSTEM_VERSION")
    } yield {
      val actorSystemName = s"${AkkaEnv.mkSystemId(bundleSystem)}-${AkkaEnv.mkSystemId(bundleSystemVersion)}"
      "play.akka.actor-system" -> actorSystemName
    }).toList
}
