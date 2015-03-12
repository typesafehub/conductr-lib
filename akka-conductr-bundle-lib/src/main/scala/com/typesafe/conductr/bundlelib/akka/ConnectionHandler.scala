/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.akka

import akka.actor._
import akka.http.client.RequestBuilding.{ Get, Post, Put, Patch, Delete, Options, Head }
import akka.http.{ Http, HttpExt }
import akka.http.model.headers.{ Host, `User-Agent` }
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import com.typesafe.conductr.bundlelib.HttpPayload
import com.typesafe.conductr.bundlelib.scala.{ AbstractConnectionHandler, AbstractConnectionContext }

import scala.concurrent.Future

object ConnectionContext {
  def apply()(implicit context: ActorRefFactory): ConnectionContext = {
    val system = actorSystemOf(context)
    apply(Http(system), ActorFlowMaterializer.create(system))
  }

  def apply(httpExt: HttpExt, actorFlowMaterializer: ActorFlowMaterializer): ConnectionContext =
    new ConnectionContext(httpExt, actorFlowMaterializer)

  /** JAVA API */
  def create(context: ActorRefFactory): ConnectionContext =
    apply()(context)

  /** JAVA API */
  def create(httpExt: HttpExt, actorFlowMaterializer: ActorFlowMaterializer): ConnectionContext =
    apply(httpExt, actorFlowMaterializer)

  private def actorSystemOf(context: ActorRefFactory): ActorSystem = {
    val system = context match {
      case s: ExtendedActorSystem => s
      case c: ActorContext        => c.system
      case null                   => throw new IllegalArgumentException("ActorRefFactory context must be defined")
      case _ =>
        throw new IllegalArgumentException(s"ActorRefFactory context must be a ActorSystem or ActorContext, got [${context.getClass.getName}]")
    }
    system
  }
}

class ConnectionContext(
  val httpExt: HttpExt,
  implicit val actorFlowMaterializer: ActorFlowMaterializer) extends AbstractConnectionContext

/**
 * Mix this trait into your Actor if you need an implicit
 * ConnectionContext in scope.
 *
 * Subclass may override `httpExt` and `actorFlowMaterializer to define custom
 * values for the `ConnectionContext`.
 */
trait ImplicitConnectionContext { this: Actor =>

  def httpExt: HttpExt =
    Http(context.system)

  def actorFlowMaterializer: ActorFlowMaterializer =
    ActorFlowMaterializer.create(context)

  final implicit val cc: ConnectionContext = ConnectionContext(httpExt, actorFlowMaterializer)
}

/**
 * INTERNAL API
 * Handles the Akka-http requests and responses
 */
private[bundlelib] class ConnectionHandler extends AbstractConnectionHandler {

  override protected type CC = ConnectionContext

  override def withConnectedRequest[T](
    payload: Option[HttpPayload])(thunk: (Int, Map[String, Option[String]]) => Option[T])(implicit cc: CC): Future[Option[T]] = {

    payload.fold[Future[Option[T]]](Future.successful(None)) { p =>
      val connection = cc.httpExt.outgoingConnection(p.getUrl.getHost, p.getUrl.getPort)
      val url = p.getUrl
      val urlStr = url.toString

      val requestMethod = p.getRequestMethod match {
        case "GET"     => Get(urlStr)
        case "POST"    => Post(urlStr)
        case "PUT"     => Put(urlStr)
        case "PATCH"   => Patch(urlStr)
        case "DELETE"  => Delete(urlStr)
        case "OPTIONS" => Options(urlStr)
        case "HEAD"    => Head(urlStr)
      }

      val request = requestMethod
        .addHeader(`User-Agent`(UserAgent))
        .addHeader(Host(url.getHost))

      val requestSource = Source.single(request)
        .via(connection)
        .map { response =>
          thunk(
            response.status.intValue(),
            response.headers.foldLeft(Map.empty[String, Option[String]]) {
              case (m, header) => m.updated(header.name(), Some(header.value()))
            })
        }
      requestSource.runWith(Sink.head())(cc.actorFlowMaterializer)
    }

  }
}
