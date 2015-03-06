/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.akka

import java.net.{ InetSocketAddress, URI, URL }

import akka.http.Http
import akka.http.model.headers.{ CacheDirectives, Location, `Cache-Control` }
import akka.http.model.{ HttpEntity, HttpResponse, StatusCodes, Uri }
import akka.http.server.Directives._
import akka.stream.ActorFlowMaterializer
import akka.testkit.TestProbe
import com.typesafe.conductr.bundlelib.scala.Env
import com.typesafe.conductr.{ AkkaUnitTest, _ }

import scala.concurrent.Await
import scala.util.{ Failure, Success }

class LocationServiceSpecWithEnv extends AkkaUnitTest("LocationServiceSpecWithEnv", "akka.loglevel = INFO") {

  "The LocationService functionality in the library" should {

    "be able to look up a named service" in {
      implicit val cc = ConnectionContext(system)
      val serviceUri = "http://service_interface:4711/known"
      withServerWithKnownService(serviceUri) {
        val service = LocationService.lookup("/known")
        Await.result(service, timeout.duration) shouldBe Some(new URI(serviceUri) -> None)
      }
    }

  }

  def withServerWithKnownService(serviceUrl: String, maxAge: Option[Int] = None)(thunk: => Unit): Unit = {
    import system.dispatcher
    implicit val materializer = ActorFlowMaterializer.create(system)

    val probe = new TestProbe(system)

    val handler =
      path("services" / Rest) { serviceName =>
        get {
          complete {
            serviceName match {
              case "known" =>
                val uri = Uri(serviceUrl)
                val headers = Location(uri) :: (maxAge match {
                  case Some(maxAgeSecs) =>
                    `Cache-Control`(
                      CacheDirectives.`private`(Location.name),
                      CacheDirectives.`max-age`(maxAgeSecs)) :: Nil
                  case None =>
                    Nil
                })
                HttpResponse(StatusCodes.TemporaryRedirect, headers, HttpEntity(s"Located at $uri"))
              case _ =>
                HttpResponse(StatusCodes.NotFound)
            }
          }
        }
      }

    val url = new URL(Env.serviceLocator.get)
    val server = Http(system).bindAndStartHandlingWith(handler, url.getHost, url.getPort, settings = None)

    try {
      server.onComplete {
        case Success(binding) => probe.ref ! binding.localAddress
        case Failure(e)       => probe.ref ! e
      }

      val address = probe.expectMsgType[InetSocketAddress]
      address.getHostString should be(url.getHost)
      address.getPort should be(url.getPort)

      thunk
    } finally {
      server.foreach(_.unbind())
    }
  }
}
