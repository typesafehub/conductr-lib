package com.typesafe.conductr.bundlelib.scala

import com.typesafe.conductr.AkkaUnitTest
import com.typesafe.conductr.bundlelib.scala.ConnectionContext.Implicits
import scala.concurrent.Await

class LocationServiceSpec extends AkkaUnitTest {

  import Implicits.global

  "The LocationService functionality in the library" should {
    "return None when running in development mode" in {
      Await.result(LocationService.lookup("/whatever"), timeout.duration) shouldBe None
    }

    "return the fallback url when running in development mode" in {
      LocationService.getLookupUrl("/whatever", "http://127.0.0.1/whatever") shouldBe "http://127.0.0.1/whatever"
    }
  }
}
