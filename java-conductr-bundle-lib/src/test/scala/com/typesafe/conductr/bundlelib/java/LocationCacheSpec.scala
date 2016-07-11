package com.typesafe.conductr.bundlelib.java

import java.net.URI
import java.time.{ Duration => JavaDuration }
import java.util.Optional
import java.util.concurrent.{ ExecutionException, CompletableFuture, CompletionStage }
import java.util.function.Supplier

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

      Await.result(cache.remove("/someservice").get(), timeout.duration) shouldBe Optional.of(new URI("/somelocation"))

      val location4 = getFromCache("/someservice")
      Await.result(location4, timeout.duration) shouldBe Optional.of(new URI("/somelocation"))
      updates shouldBe 3
    }

    "not cache location given max age not supplied" in {
      val reply: Optional[Tuple[URI, Optional[JavaDuration]]] =
        Optional.of(new Tuple(new java.net.URI("/somelocation"), Optional.empty()))
      val cache = new LocationCache()
      var updates = 0

      def getFromCache(serviceName: String): CompletionStage[Optional[URI]] =
        cache.getOrElseUpdate(serviceName, { () =>
          if (serviceName == "/other-service") {
            updates += 1
            CompletableFuture.completedFuture(reply): CompletionStage[Optional[Tuple[URI, Optional[JavaDuration]]]]
          } else
            throw new IllegalStateException(s"Some bad service: $serviceName")
        }.asJava)

      val location1 = getFromCache("/other-service")
      Await.result(location1, timeout.duration) shouldBe Optional.of(new URI("/somelocation"))
      updates shouldBe 1

      val location2 = getFromCache("/other-service")
      Await.result(location2, timeout.duration) shouldBe Optional.of(new URI("/somelocation"))
      updates shouldBe 2
    }

    "not cache empty result" in {
      var reply = Optional.empty[Tuple[java.net.URI, Optional[JavaDuration]]]
      val cache = new LocationCache()
      def getFromCache(serviceName: String): CompletionStage[Optional[URI]] =
        cache.getOrElseUpdate(serviceName, { () =>
          if (serviceName == "/other-service")
            CompletableFuture.completedFuture(reply): CompletionStage[Optional[Tuple[URI, Optional[JavaDuration]]]]
          else
            throw new IllegalStateException(s"Some bad service: $serviceName")
        }.asJava)

      Await.result(getFromCache("/other-service"), timeout.duration) shouldBe Optional.empty()

      reply = Optional.of(new Tuple(new java.net.URI("/somelocation"), Optional.of(JavaDuration.ofMillis(200))))
      Await.result(getFromCache("/other-service"), timeout.duration) shouldBe Optional.of(new URI("/somelocation"))
    }

    "not cache failure" in {
      var reply: CompletableFuture[Optional[Tuple[URI, Optional[JavaDuration]]]] = CompletableFuture.supplyAsync(new Supplier[Optional[Tuple[URI, Optional[JavaDuration]]]] {
        override def get(): Optional[Tuple[URI, Optional[JavaDuration]]] =
          throw new RuntimeException("test only")
      })

      val cache = new LocationCache()
      def getFromCache(serviceName: String): CompletionStage[Optional[URI]] =
        cache.getOrElseUpdate(serviceName, { () =>
          if (serviceName == "/other-service") {
            reply: CompletionStage[Optional[Tuple[URI, Optional[JavaDuration]]]]
          } else
            throw new IllegalStateException(s"Some bad service: $serviceName")
        }.asJava)

      val exception = intercept[ExecutionException] {
        Await.result(getFromCache("/other-service"), timeout.duration)
      }
      exception.getCause shouldBe a[RuntimeException]

      reply = CompletableFuture.completedFuture(Optional.of(new Tuple(new java.net.URI("/somelocation"), Optional.of(JavaDuration.ofMillis(200)))))
      Await.result(getFromCache("/other-service"), timeout.duration) shouldBe Optional.of(new URI("/somelocation"))
    }

  }
}
