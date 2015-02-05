/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import akka.http.Http
import akka.http.model.headers.Location
import akka.http.model.{ HttpEntity, Uri, HttpResponse, StatusCodes }
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import akka.testkit.TestProbe
import com.typesafe.conductr.AkkaUnitTest
import com.typesafe.conductr.bundlelib.Env
import java.net.{ URL, InetSocketAddress }
import scala.concurrent.Await
import scala.util.{ Failure, Success }

class LocationServiceSpecWithEnv extends AkkaUnitTest("LocationServiceSpecWithEnv", "akka.loglevel = INFO") {

  "The LocationService functionality in the library" should {
    "be able to look up a named service" in {
      import system.dispatcher
      val serviceUrl = "http://service_interface:4711/known"
      withServerWithKnownService(serviceUrl) {
        val url = LocationService.lookup("/known")
        Await.result(url, timeout.duration) should be(Some(new URL(serviceUrl)))
      }
    }

    "get back None for an unknown service" in {
      import system.dispatcher
      val serviceUrl = "http://service_interface:4711/known"
      withServerWithKnownService(serviceUrl) {
        val url = LocationService.lookup("/unknown")
        Await.result(url, timeout.duration) should be(None)
      }
    }
  }

  def withServerWithKnownService(serviceUrl: String)(thunk: => Unit): Unit = {
    import system.dispatcher
    implicit val materializer = FlowMaterializer()

    val probe = new TestProbe(system)

    val url = new URL(Env.SERVICE_LOCATOR)
    val server = Http(system).bind(url.getHost, url.getPort, settings = None)
    val mm = server.startHandlingWith(
      path("services" / Rest) { serviceName =>
        get {
          complete {
            serviceName match {
              case "known" =>
                val uri = Uri(serviceUrl)
                HttpResponse(StatusCodes.TemporaryRedirect, List(Location(uri)), HttpEntity(s"Located at $uri"))
              case _ =>
                HttpResponse(StatusCodes.NotFound)
            }
          }
        }
      })

    try {
      server.localAddress(mm).onComplete {
        case Success(address) => probe.ref ! address
        case Failure(e)       => probe.ref ! e
      }

      val address = probe.expectMsgType[InetSocketAddress]
      address.getHostString should be(url.getHost)
      address.getPort should be(url.getPort)

      thunk
    } finally {
      server.unbind(mm)
    }

  }
}
