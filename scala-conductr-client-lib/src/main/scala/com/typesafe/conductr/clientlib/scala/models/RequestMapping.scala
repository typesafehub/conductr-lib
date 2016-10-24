package com.typesafe.conductr.clientlib.scala.models

/**
 * Base type which represents request mapping for a particular endpoint.
 */
sealed trait RequestMapping

object HttpRequestMapping {

  /**
   * HTTP request mapping to be matched based on the declared HTTP request path.
   * @param path HTTP path to be matched.
   * @param method optional - HTTP method to be matched
   * @param rewrite optional - HTTP path to be rewritten to if matched
   */
  case class Path(path: String, method: Option[String] = None, rewrite: Option[String] = None) extends HttpRequestMapping

  /**
   * HTTP request mapping to be matched based for all paths that starts with given of HTTP request path.
   * @param pathBeg given HTTP path - all paths which starts with this path are matched.
   * @param method optional - HTTP method to be matched
   * @param rewrite optional - HTTP path to be rewritten to if matched
   */
  case class PathBeg(pathBeg: String, method: Option[String] = None, rewrite: Option[String] = None) extends HttpRequestMapping

  /**
   * HTTP request mapping to be matched based on a particular regex pattern.
   * @param pathRegex given regex pattern - all paths are considered matching if it matches this given pattern.
   * @param method optional - HTTP method to be matched
   * @param rewrite optional - HTTP path to be rewritten to if matched
   */
  case class PathRegex(pathRegex: String, method: Option[String] = None, rewrite: Option[String] = None) extends HttpRequestMapping
}

/**
 * Base type which represents HTTP-based request mapping.
 */
trait HttpRequestMapping extends RequestMapping {
  def method: Option[String]
  def rewrite: Option[String]
}

/**
 * Represents request mapping for a particular TCP port.
 * @param port TCP port to be mapped
 */
case class TcpRequestMapping(port: Int) extends RequestMapping

/**
 * Represents request mapping for a particular UDP port.
 * @param port UDP port to be mapped
 */
case class UdpRequestMapping(port: Int) extends RequestMapping

/**
 * Base type which represents request mapping for a given protocol family.
 */
trait ProtocolFamilyRequestMappings {
  type Mapping <: RequestMapping
  def requestMappings: Iterable[Mapping]
}

/**
 * Base type which represents request mapping for a HTTP-based protocol family (i.e. HTTP or HTTPS).
 */
case class HttpFamilyRequestMappings(requestMappings: Seq[HttpRequestMapping]) extends ProtocolFamilyRequestMappings {
  override type Mapping = HttpRequestMapping
}

/**
 * Base type which represents request mapping for a TCP protocol family.
 */
case class TcpFamilyRequestMappings(requestMappings: Set[TcpRequestMapping]) extends ProtocolFamilyRequestMappings {
  override type Mapping = TcpRequestMapping
}

/**
 * Base type which represents request mapping for a UDP protocol family.
 */
case class UdpFamilyRequestMappings(requestMappings: Set[UdpRequestMapping]) extends ProtocolFamilyRequestMappings {
  override type Mapping = UdpRequestMapping
}
