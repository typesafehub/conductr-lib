package com.typesafe.conductr.bundlelib.akka

import com.typesafe.config.{ ConfigFactory, Config }
import scala.collection.JavaConverters._

/**
 * Provides functions to set up the Akka cluster environment in accordance with what ConductR provides.
 */
object Env extends com.typesafe.conductr.bundlelib.scala.Env {

  private final val MultiValDelim = ':'

  /**
   * Represents a valid Akka system name. This can be produced by mkSystemName.
   */
  type SystemName = String

  /**
   * See asConfig(systemName)
   */
  @deprecated("Use asConfig(systemName) so that a system name is not assumed", "1.9")
  def asConfig: Config =
    asConfig(mkSystemName("application"))

  /**
   * Provides various Akka related properties, e.g. Akka seed nodes, given the environment that ConductR's provides.
   * If ConductR did not start this program there is no effect.
   *
   * If ConductR did start this then the seed node properties for an Akka cluster are automatically overridden.
   * In this case, if the AKKA_REMOTE_OTHER_IPS env is non empty then this node will join the cluster indicated by that
   * env's colon delimited seed node ips (and also AKKA_REMOTE_OTHER_PROTOCOLS and AKKA_REMOTE_OTHER_PORTS envs).
   * Note that the service name of AKKA_REMOTE is used by convention for Akka clustering, but this itself can be
   * overridden by supplying a AKKA_REMOTE_ENDPOINT_NAME env.
   *
   * Also in the case where ConductR did start this bundle but there is no AKKA_REMOTE_OTHER_IPS value then the current
   * node is assumed to be starting the cluster.
   *
   * @param systemName the system name to use - requires that mkSystemName is used
   * @return Typesafe Config that can be merged with any existing configuration - this config
   *         declares Akka remoting and cluster concerns.
   */
  def asConfig(systemName: SystemName): Config = {
    val akkaRemoteEndpointName = sys.env.getOrElse("AKKA_REMOTE_ENDPOINT_NAME", "AKKA_REMOTE")

    def presentSeedNode(protocol: String, systemName: String, ip: String, port: String, n: Int): (String, String) =
      s"akka.cluster.seed-nodes.$n" -> s"akka.$protocol://$systemName@$ip:$port"

    val akkaSeeds = {
      val akkaSeeds =
        for {
          bundleHostIp <- sys.env.get("BUNDLE_HOST_IP").toList
          akkaRemoteProtocol <- sys.env.get(s"${akkaRemoteEndpointName}_PROTOCOL")
          akkaRemoteHostPort <- sys.env.get(s"${akkaRemoteEndpointName}_HOST_PORT")
          akkaRemoteOtherProtocolsConcat <- sys.env.get(s"${akkaRemoteEndpointName}_OTHER_PROTOCOLS")
          akkaRemoteOtherIpsConcat <- sys.env.get(s"${akkaRemoteEndpointName}_OTHER_IPS")
          akkaRemoteOtherPortsConcat <- sys.env.get(s"${akkaRemoteEndpointName}_OTHER_PORTS")
        } yield if (akkaRemoteOtherProtocolsConcat.nonEmpty) {
          val akkaRemoteOtherProtocols = akkaRemoteOtherProtocolsConcat.split(MultiValDelim)
          val akkaRemoteOtherIps = akkaRemoteOtherIpsConcat.split(MultiValDelim)
          val akkaRemoteOtherPorts = akkaRemoteOtherPortsConcat.split(MultiValDelim)
          val otherAkkaRemoteNodes = for {
            (((protocol, ip), port), n) <- akkaRemoteOtherProtocols.zip(akkaRemoteOtherIps).zip(akkaRemoteOtherPorts).zipWithIndex
          } yield presentSeedNode(protocol, systemName, ip, port, n)
          otherAkkaRemoteNodes.toList
        } else
          List(presentSeedNode(akkaRemoteProtocol, systemName, bundleHostIp, akkaRemoteHostPort, 0))
      akkaSeeds.flatten
    }
    val hostname = sys.env.get("BUNDLE_HOST_IP").toList.map("akka.remote.netty.tcp.hostname" -> _)
    val port = sys.env.get(s"${akkaRemoteEndpointName}_HOST_PORT").toList.map("akka.remote.netty.tcp.port" -> _)
    val internalHostname = sys.env.get(s"${akkaRemoteEndpointName}_BIND_IP").toList.map("akka.remote.netty.tcp.bind-hostname" -> _)
    val internalPort = sys.env.get(s"${akkaRemoteEndpointName}_BIND_PORT").toList.map("akka.remote.netty.tcp.bind-port" -> _)

    ConfigFactory.parseMap((akkaSeeds ++ hostname ++ port ++ internalHostname ++ internalPort).toMap.asJava)
  }

  /**
   * Form a name for the actor system. When running from ConductR, this will comprise a name that
   * includes the system name and version.
   *
   * @param fallbackName the system name to fallback to when not running within ConductR.
   * @return the constructed system name or the fallbackName when not running within ConductR.
   */
  def mkSystemName(fallbackName: String): SystemName =
    sys.env.get("BUNDLE_SYSTEM") -> sys.env.get("BUNDLE_SYSTEM_VERSION") match {
      case (Some(bundleSystem), Some(bundleSystemVersion)) => mkSystemId(s"$bundleSystem-$bundleSystemVersion")
      case _                                               => fallbackName
    }

  /**
   * take a string representing a system name and form a valid actor system name from it.
   */
  def mkSystemId(system: String): String =
    system.dropWhile(!_.isLetterOrDigit).collect {
      case c if c.isLetterOrDigit || c == '-' || c == '_' => c
      case c if c == '.'                                  => '_'
    }
}
