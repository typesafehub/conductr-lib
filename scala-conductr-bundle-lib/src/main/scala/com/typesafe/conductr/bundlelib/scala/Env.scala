package com.typesafe.conductr.bundlelib.scala

import com.typesafe.conductr.bundlelib.{ Env => JavaEnv }

object Env extends Env

/**
 * Standard ConductR environment vars.
 */
class Env {
  /**
   * The bundle id of the current bundle
   */
  val bundleId: Option[String] =
    Option(JavaEnv.BUNDLE_ID)

  /**
   * The URL associated with reporting status back to ConductR
   */
  val conductRStatus: Option[String] =
    Option(JavaEnv.CONDUCTR_STATUS)

  /**
   * The URL associated with locating services known to ConductR
   */
  val serviceLocator: Option[String] =
    Option(JavaEnv.SERVICE_LOCATOR)

  /**
   * The universal means of determining whether ConductR started this process
   */
  val isRunByConductR: Boolean =
    JavaEnv.isRunByConductR
}
