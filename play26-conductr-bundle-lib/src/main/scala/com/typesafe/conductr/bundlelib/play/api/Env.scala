package com.typesafe.conductr.bundlelib.play.api

import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.conductr.bundlelib.akka.{ Env => AkkaEnv }

import scala.collection.JavaConverters._

/**
 * Provides functions to set up the Play environment in accordance with what ConductR provides.
 */
object Env extends com.typesafe.conductr.bundlelib.scala.Env {
  val PlayFilterHostsAllowed = "play.filters.hosts.allowed"

  /**
   * See [[asConfig()]]
   */
  def asConfig: Config =
    asConfig(AkkaEnv.mkSystemName("application"))

  def asConfig(systemName: String): Config =
    asConfig(systemName, existingConfig = None)

  def asConfig(systemName: String, existingConfig: Config): Config =
    asConfig(systemName, Some(existingConfig))

  /**
   * Provides various Play related properties.
   */
  def asConfig(systemName: String, existingConfig: Option[Config]): Config = {
    val allowedHosts = existingConfig
      .filter(_.hasPath(PlayFilterHostsAllowed))
      .fold(Seq.empty[String])(_.getStringList(PlayFilterHostsAllowed).asScala)

    val mergedConfigs = Map("play.akka.actor-system" -> systemName) ++
      (allowedHosts :+ sys.env.getOrElse("BUNDLE_HOST_IP", "127.0.0.1"))
      .zipWithIndex
      .map {
        case (host, index) => Map(s"$PlayFilterHostsAllowed.$index" -> host)
      }
      .reduce(_ ++ _)

    ConfigFactory.parseMap(mergedConfigs.asJava)
  }

}
