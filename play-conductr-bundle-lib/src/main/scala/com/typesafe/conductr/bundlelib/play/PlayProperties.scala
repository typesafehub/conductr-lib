/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.play

/**
 * Provides functions to set up the Play environment in accordance with what ConductR provides.
 */
object PlayProperties {

  /**
   * Overrides various Play related properties.
   */
  def initialize(): Unit = {
    val webName = sys.env.getOrElse("WEB_NAME", "WEB")

    val httpAddress = sys.env.get(s"${webName}_BIND_IP").toList.map("http.address" -> _)
    val httpPort = sys.env.get(s"${webName}_BIND_PORT").toList.map("http.port" -> _)

    sys.props ++= (httpAddress ++ httpPort)
  }
}
