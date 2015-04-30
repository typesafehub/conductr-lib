package com.typesafe.conductr.bundlelib

import java.net.URL

import com.typesafe.conductr.UnitTest

class HttpPayloadSpec extends UnitTest {
  "An HttpPayload" should {
    "get populated correctly" in {
      val url = new URL("http://127.0.0.1/somepath")
      val method = "GET"
      val redirects = true
      val payload = new HttpPayload(url, method, redirects)
      payload.getFollowRedirects shouldBe redirects
      payload.getRequestMethod shouldBe method
      payload.getUrl shouldBe url
    }
  }
}
