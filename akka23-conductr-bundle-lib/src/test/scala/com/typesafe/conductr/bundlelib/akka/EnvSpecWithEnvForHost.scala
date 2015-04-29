/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.akka

import com.typesafe.conductr.AkkaUnitTest

class EnvSpecWithEnvForHost extends AkkaUnitTest("EnvSpecWithEnvForHost", "akka.loglevel = INFO") {

  val config = Env.asConfig

  "The Env functionality in the library" should {
    "return seed properties when running with no other seed nodes" in {
      config.getString("akka.cluster.seed-nodes.0") shouldBe "akka.tcp://some-system-1_0_0@10.0.1.10:10000"
      config.getString("akka.remote.netty.tcp.hostname") shouldBe "10.0.1.10"
      config.getInt("akka.remote.netty.tcp.port") shouldBe 10000
    }
  }
}
