/*
 * Copyright © 2014-2016 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.clientlib.akka

import java.net.URL

import akka.{ NotUsed, Done }
import akka.actor.{ ActorRefFactory, ActorRef, FSM, Props }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{ Source, Flow }
import com.typesafe.conductr.clientlib.scala.models.Bundle
import de.heikoseeberger.akkasse.ServerSentEvent
import de.heikoseeberger.akkasse.pattern.Streams

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal
import scala.util.{ Failure, Try }

object BundlesConnector {

  /**
   * Client API which provides a nicer interface to the [[BundlesConnector]] actor.
   *
   * Returns a flow graph of `Flow[(HttpRequest, HttpRequest), Seq[Bundle], NotUsed]`.
   *
   * The input is a tuple of [[HttpRequest]]:
   * - The first element of the tuple is the HTTP request for ConductR Bundles Events endpoint.
   * - The second element of the tuple is the HTTP request for ConductR Bundles endpoint.
   *
   * The `Seq[Bundle]` will be emitted whenever there's any change of the bundle state within ConductR.
   * The change detection is derived from having Bundle SSE propagated by ConductR Bundles Events endpoint, and an
   * updated `Seq[Bundle]` being recognized by the [[BundlesConnector]] actor.
   *
   * If the timeout specified by `stopAfter` has elapsed and the stream has not yet complete, [[BundlesConnector]]
   * will terminate the stream with an error using [[TimeoutException]].
   *
   * @param conductrAddress the ConductR Control Protocol base URL
   * @param stopAfter the timeout waiting for the stream to complete.
   * @param expectingEventBurstTimeout the expected amount of time of which SSE event burst from `/bundles/events` will
   *                                   take place. Once event burst is over, the latest bundle state will be obtained
   *                                   from `/bundles/events`
   * @param reconnectInterval the amount of time to wait to reconnect to `/bundles/events` when disconnected.
   * @param system the actor system which is used to build [[BundlesConnector]]
   * @return the flow graph of `Flow[(HttpRequest, HttpRequest), Seq[Bundle], NotUsed]`.
   */
  def connect(
    conductrAddress: URL,
    stopAfter: Option[FiniteDuration] = None,
    expectingEventBurstTimeout: FiniteDuration = 500.millis,
    reconnectInterval: FiniteDuration = 500.millis
  )(implicit system: ActorRefFactory): Flow[(HttpRequest, HttpRequest), Seq[Bundle], NotUsed] =
    Flow[(HttpRequest, HttpRequest)].flatMapConcat {
      case (getBundlesEventsRequest, getBundlesRequest) =>
        Source.actorPublisher(props(conductrAddress, getBundlesEventsRequest, getBundlesRequest, stopAfter, expectingEventBurstTimeout, reconnectInterval))
    }

  private type Connection = Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]]

  sealed trait Message

  private case object ConsumeBundlesEvents extends Message
  private case class ConsumeBundlesEventsOutcome(outcome: Try[Done]) extends Message

  private case object GetBundles extends Message
  private case class GetBundlesSuccess(bundles: Seq[Bundle]) extends Message
  private case class GetBundlesError(error: Throwable) extends Message

  sealed trait State

  private object Initializing extends State
  private object ExpectingEvent extends State
  private object ExpectingEventBurst extends State

  case object TimeoutException extends RuntimeException

  private def props[T](
    conductrAddress: URL,
    getBundlesEventsRequest: HttpRequest,
    getBundlesRequest: HttpRequest,
    stopAfter: Option[FiniteDuration] = None,
    expectingEventBurstTimeout: FiniteDuration = 500.millis,
    reconnectInterval: FiniteDuration = 500.millis
  ): Props =
    Props(new BundlesConnector(conductrAddress, getBundlesEventsRequest, getBundlesRequest, stopAfter, expectingEventBurstTimeout, reconnectInterval))

  private def getBundles(connector: ActorRef, connection: Connection, request: HttpRequest)(implicit ec: ExecutionContext, mat: ActorMaterializer): Unit = {
    import JsonMarshalling._
    Source
      .single(request)
      .via(connection)
      .mapAsync(1)(Unmarshal(_).to[Seq[Bundle]])
      .runForeach(connector ! GetBundlesSuccess(_))
      .recover {
        case NonFatal(e) => connector ! GetBundlesError(e)
      }
  }

  /*
   * When consuming bundle events we request the retrieval of bundle info only when a connection
   * to the events stream is established. This ensures that we don't have a race condition whereby
   * we miss events. Otherwise, if we retrieved the bundles before being connected to the event
   * stream then we could miss out on events. We also cannot retrieve the bundle info only when
   * having received events because then startup could fail if there are none i.e. we must always
   * get the initial state of bundle info.
   */
  private def consumeBundlesEvents(connector: ActorRef, connection: Connection, request: HttpRequest)(implicit ec: ExecutionContext, mat: ActorMaterializer): Unit =
    Source
      .single(request)
      .via(connection)
      .via(
        Streams.sseFlow(
          Streams.onSuccess(_ => connector ! GetBundles),
          outcome => connector ! ConsumeBundlesEventsOutcome(outcome)
        )
      )
      .runForeach(connector ! _)

}

/**
 * Connects to the `/bundles` endpoint of ConductR and publishes the bundles from ConductR when updates from
 * the /bundles/events` SSE are available.
 *
 * If `stopAfter` is specified, [[BundlesConnector]] will terminate with [[TimeoutException]] if the bundles stream
 * is not completed within the duration specified by `stopAfter`.
 *
 * Upon startup, [[BundlesConnector]] will obtain the bundle state from the `/bundles` endpoint, and publish these
 * bundles before proceeding to connect to the `/bundle/events` endpoint of ConductR.
 */
class BundlesConnector(
    conductrAddress: URL,
    getBundlesEventsRequest: HttpRequest,
    getBundlesRequest: HttpRequest,
    stopAfter: Option[FiniteDuration],
    expectingEventBurstTimeout: FiniteDuration,
    reconnectInterval: FiniteDuration
) extends FSM[BundlesConnector.State, Seq[Bundle]] with ActorPublisher[Seq[Bundle]] {

  private implicit val mat = ActorMaterializer()

  import BundlesConnector._
  import context.dispatcher
  import context.system

  startWith(Initializing, Seq.empty)

  stopAfter.foreach(context.system.scheduler.scheduleOnce(_, self, BundlesConnector.TimeoutException))

  when(Initializing, reconnectInterval) {
    case Event(GetBundles, _) =>
      log.debug("Retrieving bundle info")
      getBundles(self, createBundlesConnection(conductrAddress), getBundlesRequest)
      stay()

    case Event(GetBundlesSuccess(newBundles), _) =>
      log.debug("Received bundles from ConductR: {}", newBundles)
      if (totalDemand > 0) {
        onNext(newBundles)
        self ! ConsumeBundlesEvents
        goto(ExpectingEvent).using(newBundles)
      } else {
        self ! GetBundles
        stay()
      }

    case Event(StateTimeout, _) =>
      log.debug("Timed out getting bundles from ConductR - retrying")
      self ! GetBundles
      stay()
  }

  when(ExpectingEvent) {
    case Event(ServerSentEvent(data, Some(_), _, _), _) =>
      goto(ExpectingEventBurst)

    case Event(GetBundles, _) =>
      log.debug("Retrieving bundle info")
      getBundles(self, createBundlesConnection(conductrAddress), getBundlesRequest)
      stay()

    case Event(GetBundlesError(e), _) =>
      log.error(e, "Retrieval of bundle info failed. Trying again shortly.")
      context.system.scheduler.scheduleOnce(reconnectInterval, self, GetBundles)
      stay()
  }

  when(ExpectingEventBurst, expectingEventBurstTimeout) {
    case Event(ServerSentEvent(data, Some(_), _, _), _) =>
      stay()

    case Event(StateTimeout, _) =>
      log.debug("Event burst over - getting info on bundles")
      self ! GetBundles
      goto(ExpectingEvent)

    case Event(GetBundles, _) =>
      log.debug("Ignoring request to retrieve bundle info while waiting for event burst")
      stay()

    case Event(GetBundlesError(e), _) =>
      log.debug("Ignoring retrieval of bundle info failed while waiting for event burst. {}", e)
      stay()
  }

  whenUnhandled {
    case Event(ServerSentEvent("", None, _, _), _) =>
      stay()

    case Event(ConsumeBundlesEvents, _) =>
      log.debug("Consuming bundles events")
      consumeBundlesEvents(self, createBundlesEventsConnection(conductrAddress), getBundlesEventsRequest)
      stay()

    case Event(ConsumeBundlesEventsOutcome(outcome), _) =>
      outcome match {
        case Failure(reason) => log.error(reason, "Bundles events connection closed")
        case _               => log.debug("Bundles events connection closed")
      }
      context.system.scheduler.scheduleOnce(reconnectInterval, self, ConsumeBundlesEvents)
      goto(ExpectingEvent)

    case Event(GetBundlesSuccess(newBundles), bundles) if newBundles != bundles =>
      log.debug("Received changed bundles from ConductR: {}", newBundles)
      if (totalDemand > 0)
        onNext(newBundles)

      stay().using(newBundles)

    case Event(GetBundlesSuccess(newBundles), _) =>
      log.debug("Received unchanged bundles from ConductR: {}", newBundles)
      stay()

    case Event(v @ BundlesConnector.TimeoutException, _) =>
      onError(v)
      stop()
  }

  initialize()

  self ! GetBundles

  protected def createBundlesConnection(conductrAddress: URL) = {
    val (host, port) = conductrHostAndPort(conductrAddress)
    Http().outgoingConnection(host, port)
  }

  protected def createBundlesEventsConnection(conductrAddress: URL) = {
    val (host, port) = conductrHostAndPort(conductrAddress)
    Http().outgoingConnection(host, port)
  }

  protected def conductrHostAndPort(conductrAddress: URL): (String, Int) = {
    val port = if (conductrAddress.getPort != -1) conductrAddress.getPort else conductrAddress.getDefaultPort
    conductrAddress.getHost -> port
  }
}
