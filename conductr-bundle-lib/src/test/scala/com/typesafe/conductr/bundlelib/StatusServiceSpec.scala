package com.typesafe.conductr.bundlelib

import com.typesafe.conductr.UnitTest

class StatusServiceSpec extends UnitTest {

  "The StatusService functionality in the library" should {

    "not fail when running in development mode" in {
      StatusService.createSignalStartedPayload() shouldBe null
    }
  }
}
