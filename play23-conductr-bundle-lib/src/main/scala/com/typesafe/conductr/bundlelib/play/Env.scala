package com.typesafe.conductr.bundlelib.play

import com.typesafe.conductr.bundlelib.akka.{ Env => AkkaEnv }

import com.typesafe.config.{ ConfigFactory, Config }
import scala.collection.JavaConverters._

/**
 * Provides functions to set up the Play environment in accordance with what ConductR provides.
 */
object Env extends com.typesafe.conductr.bundlelib.scala.Env {

  /**
   * Provides various Play related properties.
   */
  def asConfig: Config = {
    val webName = sys.env.getOrElse("WEB_NAME", "WEB")

    val httpAddress = sys.env.get(s"${webName}_BIND_IP").toList.map("http.address" -> _)
    val httpPort = sys.env.get(s"${webName}_BIND_PORT").toList.map("http.port" -> _)

    val playActorSystem = sys.env.get("BUNDLE_SYSTEM").toList.map("play.akka.actor-system" -> AkkaEnv.mkSystem(_))

    ConfigFactory.parseMap((httpAddress ++ httpPort ++ playActorSystem).toMap.asJava)
  }
}
