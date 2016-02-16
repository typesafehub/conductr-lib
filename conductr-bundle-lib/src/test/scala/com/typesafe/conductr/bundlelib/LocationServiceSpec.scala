package com.typesafe.conductr.bundlelib

import _root_.java.net.URL

import com.typesafe.conductr.lib.UnitTest

class LocationServiceSpec extends UnitTest {

  "The LocationService functionality in the library" should {
    "return null when running in development mode" in {
      LocationService.createLookupPayload("/whatever") shouldBe null
    }

    "return the fallback when running in development mode" in {
      val fallback = new URL("http://127.0.0.1/whatever")
      LocationService.getLookupUrl("/whatever", fallback) shouldBe fallback
    }
  }
}
