package com.typesafe.conductr.bundlelib.java

import java.net.URI
import java.time.{ Duration => JavaDuration }
import java.util.Optional
import java.util.concurrent.{ CompletableFuture, CompletionStage }

import com.typesafe.conductr.lib.AkkaUnitTest

import scala.compat.java8.FunctionConverters._

import com.typesafe.conductr.lib.java._

class LocationCacheSpec extends AkkaUnitTest {

  "A location cache" should {
    "invoke the op when there is no entry initially, return the cached entry when not, and expire" in {
      var updates = 0
      val cache = new LocationCache()
      def getFromCache(serviceName: String): CompletionStage[Optional[URI]] =
        cache.getOrElseUpdate(serviceName, { () =>
          if (serviceName == "/someservice") {
            updates += 1
            CompletableFuture.completedFuture(
              Optional.of(new Tuple(new URI("/somelocation"), Optional.of(JavaDuration.ofMillis(200))))
            ): CompletionStage[Optional[Tuple[URI, Optional[JavaDuration]]]]
          } else
            throw new IllegalStateException(s"Some bad service: $serviceName")
        }.asJava)

      val location1 = getFromCache("/someservice")
      Await.result(location1, timeout.duration) shouldBe Optional.of(new URI("/somelocation"))
      updates shouldBe 1

      // Shouldn't invoke the op for this - it should just return the cache value
      val location2 = getFromCache("/someservice")
      Await.result(location2, timeout.duration) shouldBe Optional.of(new URI("/somelocation"))
      updates shouldBe 1

      // Let the cache expire
      Thread.sleep(500)

      val location3 = getFromCache("/someservice")
      Await.result(location3, timeout.duration) shouldBe Optional.of(new URI("/somelocation"))
      updates shouldBe 2

      cache.remove("/someservice") shouldBe Optional.of(location3)
      val location4 = getFromCache("/someservice")
      Await.result(location4, timeout.duration) shouldBe Optional.of(new URI("/somelocation"))
      updates shouldBe 3
    }
  }
}
