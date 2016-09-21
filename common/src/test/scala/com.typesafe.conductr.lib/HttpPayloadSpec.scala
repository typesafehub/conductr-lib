package com.typesafe.conductr.lib

import _root_.java.net.URL
import _root_.scala.collection.JavaConversions._

class HttpPayloadSpec extends UnitTest {
  "An HttpPayload" should {
    "get populated correctly" in {
      val url = new URL("http://127.0.0.1/somepath")
      val method = "GET"
      val redirects = true
      val headers = Map("Accept" -> "multipart/form-data")
      val payload = new HttpPayload(url, method, redirects, headers)
      payload.getFollowRedirects shouldBe redirects
      payload.getRequestMethod shouldBe method
      payload.getUrl shouldBe url
      payload.getRequestHeaders.toMap shouldBe headers
    }
  }
}
