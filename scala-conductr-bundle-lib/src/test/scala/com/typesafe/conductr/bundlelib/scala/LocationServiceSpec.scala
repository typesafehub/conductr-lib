package com.typesafe.conductr.bundlelib.scala

import com.typesafe.conductr.AkkaUnitTest
import com.typesafe.conductr.bundlelib.scala.ConnectionContext.Implicits
import scala.concurrent.Await

class LocationServiceSpec extends AkkaUnitTest {

  import Implicits.global

  "The LocationService functionality in the library" should {
    "return the fallback uri when running in development mode" in {
      val fallback = URI("/fallback")
      val cache = LocationCache()
      Await.result(LocationService.lookup("/whatever", fallback, cache), timeout.duration) shouldBe Some(fallback)
    }

    "return the fallback url when running in development mode" in {
      val fallback = URL("http://127.0.0.1/whatever")
      LocationService.getLookupUrl("/whatever", fallback) shouldBe fallback
    }
  }
}
