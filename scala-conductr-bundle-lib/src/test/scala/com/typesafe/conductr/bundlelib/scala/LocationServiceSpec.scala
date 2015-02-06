/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import java.net.URL

import com.typesafe.conductr.AkkaUnitTest
import scala.concurrent.Await

class LocationServiceSpec extends AkkaUnitTest("StatusServiceSpec", "akka.loglevel = INFO") {

  "The LocationService functionality in the library" should {
    "return None when running in development mode" in {
      import system.dispatcher
      Await.result(LocationService.lookup("/whatever"), timeout.duration) should be(None)
    }

    "return a default URL when running in development mode" in {
      val default = new URL("http://127.0.0.1:9000")
      LocationService.getUrlOrExit(default)(None) should be(default)
    }
  }
}
