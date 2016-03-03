package com.typesafe.conductr.bundlelib.lagom

import java.util.function.{ Function => JFunction }
import javax.inject.{ Inject }

import com.typesafe.conductr.bundlelib.scala.{ CacheLike }
import com.lightbend.lagom.javadsl.api.ServiceLocator
import scala.concurrent.Future
import com.typesafe.conductr.bundlelib.scala.URI
import java.net.{ URI => JavaURI }
import java.util.Optional
import java.util.concurrent.CompletionStage
import scala.language.reflectiveCalls
import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._
import com.typesafe.conductr.bundlelib.play.api.LocationService

/**
 * ConductRServiceLocator implements Lagom's ServiceLocator by using the ConductR Service Locator.
 */
class ConductRServiceLocator @Inject() (locationService: LocationService, cache: CacheLike) extends ServiceLocator {

  import locationService.cc.executionContext

  private def locateAsScala(name: String): Future[Optional[JavaURI]] =
    locationService.lookup(name, URI(""), cache).map(_.asJava)

  override def locate(name: String): CompletionStage[Optional[JavaURI]] =
    locateAsScala(name).toJava

  override def doWithService[T](name: String, block: JFunction[JavaURI, CompletionStage[T]]): CompletionStage[Optional[T]] =
    locateAsScala(name).flatMap(uriOpt => {
      if (uriOpt.isPresent())
        block.apply(uriOpt.get()).toScala.map(Optional.of(_))
      else
        Future.successful(Optional.empty[T])
    }).toJava
}
