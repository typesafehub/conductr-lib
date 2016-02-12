package com.typesafe.conductr.bundlelib.java

import java.util.Optional

import com.typesafe.conductr.lib.AkkaUnitTest
import com.typesafe.conductr.lib.java._

class StatusServiceSpec extends AkkaUnitTest("StatusServiceSpec", "akka.loglevel = INFO") {

  "The StatusService functionality in the library" should {
    "return None when running in development mode" in {
      Await.result(StatusService.signalStartedOrExit(), timeout.duration) shouldBe Optional.empty()
      Await.result(StatusService.signalStarted(), timeout.duration) shouldBe Optional.empty()
    }
  }
}
