/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe

import akka.http.HttpExt
import akka.http.engine.server.ServerSettings
import akka.http.model.{ HttpRequest, HttpResponse }
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{ Flow, Sink }

package object conductr {

  /**
   * TODO: Remove after https://github.com/akka/akka/issues/16972 is fixed
   */
  implicit class HttpExtOps(http: HttpExt) {
    def bindAndStartHandlingWith(handler: Flow[HttpRequest, HttpResponse, _], interface: String, port: Int, settings: Option[ServerSettings] = None)(implicit fm: ActorFlowMaterializer) =
      http.bind(interface, port, settings = settings).to(Sink.foreach { conn ⇒
        conn.flow.join(handler).run()
      }).run()
  }

}
