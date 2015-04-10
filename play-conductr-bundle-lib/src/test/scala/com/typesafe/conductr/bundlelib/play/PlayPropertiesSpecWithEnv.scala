/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.play

import com.typesafe.conductr.AkkaUnitTest

class PlayPropertiesSpecWithEnv extends AkkaUnitTest("AkkaPropertiesSpecWithEnvForHost", "akka.loglevel = INFO") {

  PlayProperties.initialize()

  "The PlayProperties functionality in the library" should {
    "initialize the env like expected" in {
      sys.props.get("http.address") shouldBe Some("127.0.0.1")
      sys.props.get("http.port") shouldBe Some("9000")
    }
  }
}
