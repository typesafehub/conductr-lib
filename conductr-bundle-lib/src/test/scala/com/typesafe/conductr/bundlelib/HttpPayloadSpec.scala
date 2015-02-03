/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

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
      payload.getFollowRedirects should be(redirects)
      payload.getRequestMethod should be(method)
      payload.getUrl should be(url)
    }
  }
}
