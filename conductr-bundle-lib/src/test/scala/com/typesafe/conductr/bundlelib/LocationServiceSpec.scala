package com.typesafe.conductr.bundlelib

import com.typesafe.conductr.UnitTest

class LocationServiceSpec extends UnitTest {

  "The LocationService functionality in the library" should {
    "return null when running in development mode" in {
      LocationService.createLookupPayload("/whatever") shouldBe null
    }

    "return the fallback when running in development mode" in {
      LocationService.getLookupUrl("/whatever", "http://127.0.0.1/whatever") shouldBe "http://127.0.0.1/whatever"
    }
  }
}
