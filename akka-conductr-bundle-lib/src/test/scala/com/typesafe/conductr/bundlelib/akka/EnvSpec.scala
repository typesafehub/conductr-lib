/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.akka

import com.typesafe.conductr.AkkaUnitTest
import com.typesafe.config.ConfigException.Missing

class EnvSpec extends AkkaUnitTest("EnvSpec", "akka.loglevel = INFO") {

  val config = Env.asConfig

  "The Env functionality in the library" should {
    "return no seed properties when running in development mode" in {
      intercept[Missing](config.getString("akka.cluster.seed-nodes.0"))
      intercept[Missing](config.getString("akka.remote.netty.tcp.hostname"))
      intercept[Missing](config.getString("akka.remote.netty.tcp.port"))
    }
  }
}
