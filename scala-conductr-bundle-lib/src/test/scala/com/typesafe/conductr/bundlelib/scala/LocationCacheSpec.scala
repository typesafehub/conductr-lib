/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import com.typesafe.conductr.AkkaUnitTest

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class LocationCacheSpec extends AkkaUnitTest {

  "A location cache" should {
    "invoke the op when there is no entry initially, return the cached entry when not, and expire" in {
      var updates = 0
      val cache = LocationCache()
      def getFromCache(serviceName: String): Future[Option[String]] =
        cache.getOrElseUpdate(serviceName) {
          if (serviceName == "/someservice") {
            updates += 1
            Future.successful(Some("/somelocation" -> Some(100.millis)))
          } else
            throw new IllegalStateException(s"Some bad service: $serviceName")
        }

      val location1 = getFromCache("/someservice")
      Await.result(location1, timeout.duration) shouldBe Some("/somelocation")
      updates shouldBe 1

      // Shouldn't invoke the op for this - it should just return the cache value
      val location2 = getFromCache("/someservice")
      Await.result(location2, timeout.duration) shouldBe Some("/somelocation")
      updates shouldBe 1

      // Let the cache expire
      Thread.sleep(200)

      val location3 = getFromCache("/someservice")
      Await.result(location3, timeout.duration) shouldBe Some("/somelocation")
      updates shouldBe 2

      cache.remove("/someservice") shouldBe Some(location3)
      val location4 = getFromCache("/someservice")
      Await.result(location4, timeout.duration) shouldBe Some("/somelocation")
      updates shouldBe 3
    }
  }
}
