package com.typesafe.conductr.bundlelib.scala

import com.typesafe.conductr.lib.AkkaUnitTest
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class LocationCacheSpec extends AkkaUnitTest with ScalaFutures {

  "A location cache" should {
    "invoke the op when there is no entry initially, return the cached entry when not, and expire" in {
      var updates = 0
      val cache = LocationCache()
      def getFromCache(serviceName: String): Future[Option[java.net.URI]] =
        cache.getOrElseUpdate(serviceName) {
          if (serviceName == "/someservice") {
            updates += 1
            Future.successful(Some(URI("/somelocation") -> Some(200.millis)))
          } else
            throw new IllegalStateException(s"Some bad service: $serviceName")
        }

      val location1 = getFromCache("/someservice")
      Await.result(location1, timeout.duration) shouldBe Some(URI("/somelocation"))
      updates shouldBe 1

      // Shouldn't invoke the op for this - it should just return the cache value
      val location2 = getFromCache("/someservice")
      Await.result(location2, timeout.duration) shouldBe Some(URI("/somelocation"))
      updates shouldBe 1

      // Let the cache expire
      Thread.sleep(500)

      val location3 = getFromCache("/someservice")
      Await.result(location3, timeout.duration) shouldBe Some(URI("/somelocation"))
      updates shouldBe 2

      cache.remove("/someservice").get.futureValue shouldBe Some(URI("/somelocation"))
      val location4 = getFromCache("/someservice")
      Await.result(location4, timeout.duration) shouldBe Some(URI("/somelocation"))
      updates shouldBe 3
    }

    "not cache location if maxAge is not supplied" in {
      val cache = LocationCache()
      var updates = 0
      def getFromCache(serviceName: String): Future[Option[java.net.URI]] =
        cache.getOrElseUpdate(serviceName) {
          if (serviceName == "/other-service") {
            updates += 1
            Future.successful(Some(new java.net.URI("/somelocation") -> None))
          } else
            throw new IllegalStateException(s"Some bad service: $serviceName")
        }

      val location1 = getFromCache("/other-service")
      Await.result(location1, timeout.duration) shouldBe Some(URI("/somelocation"))
      updates shouldBe 1

      val location2 = getFromCache("/other-service")
      Await.result(location2, timeout.duration) shouldBe Some(URI("/somelocation"))
      updates shouldBe 2
    }

    "not cache empty result" in {
      var reply = Option.empty[(java.net.URI, Option[FiniteDuration])]
      val cache = LocationCache()
      def getFromCache(serviceName: String): Future[Option[java.net.URI]] =
        cache.getOrElseUpdate(serviceName) {
          if (serviceName == "/other-service")
            Future.successful(reply)
          else
            throw new IllegalStateException(s"Some bad service: $serviceName")
        }

      Await.result(getFromCache("/other-service"), timeout.duration) shouldBe None

      reply = Some(new java.net.URI("/somelocation") -> Some(200.millis))
      Await.result(getFromCache("/other-service"), timeout.duration) shouldBe Some(URI("/somelocation"))

    }

    "not cache failure" in {
      var reply: Future[Option[(java.net.URI, Option[FiniteDuration])]] = Future.failed(new RuntimeException("test only"))
      val cache = LocationCache()
      def getFromCache(serviceName: String): Future[Option[java.net.URI]] =
        cache.getOrElseUpdate(serviceName) {
          if (serviceName == "/other-service")
            reply
          else
            throw new IllegalStateException(s"Some bad service: $serviceName")
        }

      getFromCache("/other-service").failed.futureValue shouldBe a[RuntimeException]

      reply = Future.successful(Some(new java.net.URI("/somelocation") -> Some(200.millis)))
      Await.result(getFromCache("/other-service"), timeout.duration) shouldBe Some(URI("/somelocation"))

    }
  }
}
