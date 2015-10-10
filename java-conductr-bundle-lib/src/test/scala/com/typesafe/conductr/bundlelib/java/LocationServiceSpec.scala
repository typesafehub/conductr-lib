package com.typesafe.conductr.bundlelib.java

import java.net.{ URL, URI }
import java.util.Optional

import com.typesafe.conductr.AkkaUnitTest
import com.typesafe.conductr.java.Await

class LocationServiceSpec extends AkkaUnitTest {

  "The LocationService functionality in the library" should {
    "return the fallback uri when running in development mode" in {
      val fallback = new URI("/fallback")
      val cache = new LocationCache()
      Await.result(LocationService.lookup("/whatever", fallback, cache), timeout.duration) shouldBe Optional.of(fallback)
    }

    "return the fallback url when running in development mode" in {
      val fallback = new URL("http://127.0.0.1/whatever")
      LocationService.getLookupUrl("/whatever", fallback) shouldBe fallback
    }
  }
}
