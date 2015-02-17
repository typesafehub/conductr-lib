/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import com.typesafe.conductr.bundlelib.{ Env => JavaEnv }

/**
 * Standard ConductR environment vars.
 */
object Env {
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
