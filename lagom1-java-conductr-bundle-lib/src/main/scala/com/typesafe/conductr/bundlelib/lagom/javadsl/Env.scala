package com.typesafe.conductr.bundlelib.lagom.javadsl

import com.typesafe.config.{ Config, ConfigFactory }

import scala.collection.JavaConverters._

/**
 * Provides functions to set up the Play environment in accordance with what ConductR provides.
 */
object Env extends com.typesafe.conductr.bundlelib.scala.Env {

  /**
   * Provides various Lagom related properties.
   */
  def asConfig: Config =
    ConfigFactory.parseMap(lagomCassandraKeyspace.asJava)

  private final val CassandraKeyspaceNameRegex = """^("[a-zA-Z]{1}[\w]{0,47}"|[a-zA-Z]{1}[\w]{0,47})$"""
  private def isValidKeyspaceName(name: String): Boolean =
    name.matches(CassandraKeyspaceNameRegex)

  private def lagomCassandraKeyspace: Map[String, String] =
    Env.bundleName.fold(Map.empty[String, String]) { bundleName =>
      val keyspace =
        if (isValidKeyspaceName(bundleName))
          bundleName
        else {
          // I'm confident the normalized name will work in most situations. If it doesn't, then
          // the application will fail at runtime and users will have to provide a valid keyspace
          // name in the application.conf
          bundleName.replaceAll("""[^\w]""", "_")
        }
      Map(
        "cassandra-journal.defaults.keyspace" -> keyspace,
        "cassandra-snapshot-store.defaults.keyspace" -> keyspace,
        "lagom.defaults.persistence.read-side.cassandra.keyspace" -> keyspace
      )
    }
}
