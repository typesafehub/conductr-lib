/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.play

import com.typesafe.conductr.AkkaUnitTest

class EnvSpecWithEnv extends AkkaUnitTest("EnvSpecWithEnvForHost", "akka.loglevel = INFO") {

  val config = Env.asConfig

  "The Env functionality in the library" should {
    "initialize the env like expected" in {
      config.getString("http.address") shouldBe "127.0.0.1"
      config.getString("http.port") shouldBe "9000"
      config.getString("play.akka.actor-system") shouldBe "somesys"
    }
  }
}
