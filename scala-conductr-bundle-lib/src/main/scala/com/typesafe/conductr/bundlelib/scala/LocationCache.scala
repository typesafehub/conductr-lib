package com.typesafe.conductr.bundlelib.scala

import java.net.{ URI => JavaURI }
import java.util.{ TimerTask, Timer }

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Success

/**
 * A structure that describes what we require from a cache specifically for locations in relation to service names.
 * This interface describes an expiring entry cache inspired by Spray Cache in its elements being captured as Futures, thus
 * being able to cope with the thundering herds issue:
 * http://ehcache.org/documentation/2.8/recipes/thunderingherd.
 *
 * Entries that provide a max age duration are scheduled to be removed at that time. The
 * expectation is that this cache is used with such durations. Where there is no duration
 * (this should be rare) then the cache entry is quickly removed after it has been determined.
 * This removal also occurs when the entry cannot be established successfully
 */
trait CacheLike {
  def getOrElseUpdate(serviceName: String)(op: => Future[Option[(JavaURI, Option[FiniteDuration])]]): Future[Option[JavaURI]]
  def remove(serviceName: String): Option[Future[Option[JavaURI]]]
}

object LocationCache {
  def apply() = new LocationCache
}

/**
 * A cache like class as a default implementation.
 */
class LocationCache extends CacheLike {

  private val cache = TrieMap.empty[String, Future[Option[(JavaURI, Option[FiniteDuration])]]]

  val reaperTimer = new Timer("LocationCache-Reaper", true /* daemon thread */ )

  override def getOrElseUpdate(serviceName: String)(op: => Future[Option[(JavaURI, Option[FiniteDuration])]]): Future[Option[JavaURI]] = {
    import Implicits.global
    cache.getOrElseUpdate(serviceName, op)
      .andThen {
        case Success(Some((location, Some(maxAge)))) =>
          reaperTimer.schedule(new TimerTask {
            override def run(): Unit = {
              cache.remove(serviceName)
            }
          }, maxAge.toMillis)

        case _ =>
          cache.remove(serviceName)
      }
      .map(_.map(_._1))

  }

  override def remove(serviceName: String): Option[Future[Option[JavaURI]]] = {
    import Implicits.global
    cache.remove(serviceName)
      .map(_.map(_.map(_._1)))
  }
}
