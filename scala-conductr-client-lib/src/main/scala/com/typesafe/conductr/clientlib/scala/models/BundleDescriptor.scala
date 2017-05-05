package com.typesafe.conductr.clientlib.scala.models

import java.util

import com.typesafe.config._

import scala.collection.JavaConverters._
import scala.collection.breakOut
import scala.util.Try

object BundleDescriptor {

  val EmptyAnnotations: ConfigObject =
    ConfigValueFactory.fromMap(new util.HashMap[String, Object]())

  case class MalformedBundleConfException(description: String) extends Exception(description)

  object Component {

    sealed trait FileSystemType

    object FileSystemType {
      case object Universal extends FileSystemType
      case object Docker extends FileSystemType

      def apply(s: String): FileSystemType =
        s.toLowerCase match {
          case "universal" => Universal
          case "docker"    => Docker
        }
    }

    case class Endpoint(
      bindProtocol: String,
      bindPort: Int,
      serviceName: Option[String],
      requestAcls: Seq[RequestAcl]
    )

    def apply(config: Config): Component = {
      val endpointsObj = config.getObject("endpoints")
      Component(
        config.getString("description"),
        FileSystemType(config.getString("file-system-type")),
        config.getStringList("start-command").asScala.toList,
        endpointsObj.keySet.asScala.map { name =>
          val endpoint = endpointsObj.get(name).asInstanceOf[ConfigObject].toConfig

          val serviceName = if (endpoint.hasPath("service-name"))
            Option(endpoint.getString("service-name")).filter(_.nonEmpty)
          else
            None

          val requestAcls = if (endpoint.hasPath("acls"))
            endpoint.getConfigList("acls").asScala.map(parseRequestAcl)
          else
            Seq.empty[RequestAcl]

          name -> Endpoint(
            endpoint.getString("bind-protocol"),
            endpoint.getInt("bind-port"),
            serviceName,
            requestAcls
          )
        }(breakOut)
      )
    }

    private def parseRequestAcl(config: Config): RequestAcl =
      RequestAcl(
        config.entrySet().asScala
          .map(_.getKey.split('.').toSeq)
          .collect {
            case Seq(protocol @ "http", configKey @ "requests") =>
              val requestMappingsConfigs = config.getConfig(protocol).getConfigList(configKey).asScala.toList
              HttpFamilyRequestMappings(requestMappingsConfigs.map(parseHttpRequestMapping))

            case Seq(protocol @ "tcp", configKey @ "requests") =>
              val ports = config.getConfig(protocol).getIntList(configKey)
              TcpFamilyRequestMappings(ports.asScala.map(TcpRequestMapping(_)))

            case Seq(protocol @ "udp", configKey @ "requests") =>
              val ports = config.getConfig(protocol).getIntList(configKey)
              UdpFamilyRequestMappings(ports.asScala.map(UdpRequestMapping(_)))
          }
          .foldLeft(Seq.empty[ProtocolFamilyRequestMappings])(_ :+ _)
      )

    private def parseHttpRequestMapping(config: Config): HttpRequestMapping = {
      def getOptString(key: String): Option[String] =
        if (config.hasPath(key)) Some(config.getString(key)) else None

      val method = getOptString("method")

      (getOptString("path"), getOptString("path-beg"), getOptString("path-regex"), getOptString("rewrite")) match {
        case (Some(path), None, None, rewrite) =>
          HttpRequestMapping.Path(path, method, rewrite)
        case (None, Some(pathBeg), None, rewrite) =>
          HttpRequestMapping.PathBeg(pathBeg, method, rewrite)
        case (None, None, Some(pathRegex), rewrite) =>
          HttpRequestMapping.PathRegex(pathRegex, method, rewrite)
        case _ =>
          throw MalformedBundleConfException(s"Either path, path-beg, or path-regex must be present")
      }
    }
  }

  /**
   * Represents a component in a bundle.conf file.
   */
  case class Component(
    description: String,
    fileSystemType: Component.FileSystemType,
    startCommand: Seq[String],
    endpoints: Map[String, Component.Endpoint]
  )

  final val BundleConf = "bundle.conf"

  object ConfigKeys {

    // Support camel case identifiers for backward compatibility pre 2.1.
    private def camelCase(id: String): String =
      id
        .foldLeft((new StringBuilder(id.length), false)) {
          case ((a, _), c) if c == '-' => (a, true)
          case ((a, n), c) if n        => (a.append(c.toUpper), false)
          case ((a, _), c)             => (a.append(c), false)
        }
        ._1
        .toString

    final val Version = "version"
    final val System = "system"
    final val SystemVersion = "system-version"
    final val SystemVersionCamelCase = camelCase(SystemVersion)
    final val NrOfCpus = "nr-of-cpus"
    final val NrOfCpusCamelCase = camelCase(NrOfCpus)
    final val Memory = "memory"
    final val DiskSpace = "disk-space"
    final val DiskSpaceCamelCase = camelCase(DiskSpace)
    final val Roles = "roles"
    final val BundleName = "name"
    final val CompatibilityVersion = "compatibility-version"
    final val CompatibilityVersionCamelCase = camelCase(CompatibilityVersion)
    final val Tags = "tags"
    final val Annotations = "annotations"
  }

  def fromConfig(input: ConfigObject): BundleDescriptor = {
    import ConfigKeys._

    val config = input.toConfig
    val componentsObj = config.getObject("components")
    val system = config.getString(System)
    BundleDescriptor(
      config.getString(Version),
      system,
      Try(config.getString(SystemVersion)).getOrElse(config.getString(SystemVersionCamelCase)),
      Try(config.getDouble(NrOfCpus)).getOrElse(config.getDouble(NrOfCpusCamelCase)),
      config.getLong(Memory),
      Try(config.getLong(DiskSpace)).getOrElse(config.getLong(DiskSpaceCamelCase)),
      config.getStringList(Roles).asScala,
      config.getString(BundleName),
      Try(config.getString(CompatibilityVersion)).getOrElse(config.getString(CompatibilityVersionCamelCase)),
      Try(config.getStringList(Tags).asScala.to[List]).getOrElse(List.empty),
      if (config.hasPath(Annotations)) Some(Try(config.getObject(Annotations)).getOrElse(EmptyAnnotations)) else None,
      componentsObj.keySet.asScala.map { name =>
        name -> Component(componentsObj.get(name).asInstanceOf[ConfigObject].toConfig)
      }(breakOut)
    )
  }

}

/**
 * Represents a bundle.conf file.
 */
case class BundleDescriptor(
  version: String,
  system: String,
  systemVersion: String,
  nrOfCpus: Double,
  memory: Long,
  diskSpace: Long,
  roles: Seq[String],
  bundleName: String,
  compatibilityVersion: String,
  tags: Seq[String],
  annotations: Option[ConfigObject],
  components: Map[String, BundleDescriptor.Component]
)

