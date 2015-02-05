/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import akka.http.Http
import akka.http.model.StatusCodes
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import akka.testkit.TestProbe
import com.typesafe.conductr.AkkaUnitTest
import com.typesafe.conductr.bundlelib.Env
import java.net.{ InetSocketAddress, URL }
import scala.concurrent.Await
import scala.util.{ Failure, Success }

class StatusServiceSpecWithEnv extends AkkaUnitTest("StatusServiceSpecWithEnv", "akka.loglevel = INFO") {

  "The StatusService functionality in the library" should {
    "be able to call the right URL to signal that it is up" in {
      import system.dispatcher
      implicit val materializer = FlowMaterializer()

      val probe = new TestProbe(system)

      val url = new URL(Env.CONDUCTR_STATUS)
      val server = Http(system).bind(url.getHost, url.getPort, settings = None)
      val mm = server.startHandlingWith(
        path("bundles" / Segment) { bundleId =>
          put {
            parameter('isStarted ! "true") {
              complete {
                probe.ref ! bundleId
                StatusCodes.NoContent
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

        Await.result(StatusService.signalStarted(), timeout.duration).isDefined should be(true)

        val receivedId = probe.expectMsgType[String]
        receivedId should be(Env.BUNDLE_ID)
      } finally {
        server.unbind(mm)
      }
    }
  }
}
