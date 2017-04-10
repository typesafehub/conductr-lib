package com.typesafe.conductr.clientlib.akka

import java.io._
import java.net.{ URI, URL }
import java.nio.file.Paths
import java.util.zip.ZipInputStream

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpEntity.IndefiniteLength
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.CacheDirectives.`no-cache`
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.http.scaladsl.unmarshalling.{ PredefinedFromEntityUnmarshallers, Unmarshal }
import akka.stream.scaladsl.{ FileIO, Sink, Source }
import akka.util.ByteString
import com.typesafe.conductr.lib.HttpPayload
import com.typesafe.conductr.lib.akka.{ ConnectionContext, ConnectionHandler }
import com.typesafe.conductr.clientlib.akka.models._
import com.typesafe.conductr.clientlib.scala.models._
import com.typesafe.conductr.clientlib.scala.{ AbstractControlClient, withCloseable, withZipInputStream }
import com.typesafe.config.{ ConfigFactory, ConfigObject }
import de.heikoseeberger.akkasse.{ EventStreamUnmarshalling, ServerSentEvent }
import org.reactivestreams.{ Subscriber, Publisher }
import play.api.libs.json.{ Json, Reads }
import scala.concurrent.duration._
import scala.concurrent.{ Future, blocking }

/**
 * Factory to retrieve the ConductR control client instance.
 * As a ConductR's REST API `v2` is used.
 */
object ControlClient {

  /** SCALA API **/
  def apply(conductrAddress: URL): ControlClient =
    new ControlClient(new ConnectionHandler, conductrAddress, ApiVersion.V2)

  /** JAVA API **/
  def create(conductrAddress: URL): ControlClient =
    ControlClient(conductrAddress)
}

/**
 * Akka 2.3 flavor of the [[AbstractControlClient]].
 * Uses Akka HTTP and Akka streams to interact with ConductR.
 *
 * @param handler The connection handler contains the logic how to interact with the ConductR HTTP server
 * @param conductrAddress contains the protocol://host:port of the ConductR control server
 * @param apiVersion The version of ConductR's REST API
 */
class ControlClient(handler: ConnectionHandler, conductrAddress: URL, apiVersion: ApiVersion.Value)
    extends AbstractControlClient(conductrAddress) {

  import EventStreamUnmarshalling._
  import JsonMarshalling._

  override protected type CC = ConnectionContext

  private final val DefaultEventsCount = 10
  private final val DefaultLogsCount = 10
  private final val DefaultScale = 1

  /**
   * @see [[AbstractControlClient.getBundlesInfo()]]
   * @param cc implicit connection context
   * @return the bundles
   */
  override def getBundlesInfo()(implicit cc: ConnectionContext): Future[Seq[Bundle]] =
    handler.withConnectedRequest(Payload.bundlesInfo)(handleAsSeq[Bundle])

  /**
   * @see [[AbstractControlClient.getBundle()]]
   * @param bundleId An existing bundle identifier, a shortened version of it (min 7 characters) or
   *                 a non-ambiguous name given to the bundle during loading.
   * @param bundleData A byte array subscriber to which the bundle data will be streamed into.
   * @param configData A byte array subscriber to which the config data will be streamed into.
   * @param cc implicit connection context
   * @return The result as a Future[BundleGetResult]. BundleGetResult is a sealed trait and can be either:
   *         - BundleGetSuccess if the get bundle request has been succeeded. This object contains the bundle id, bundle file, and optionally the config file.
   *         - BundleGetFailure if the get bundle request has been failed. This object contains the HTTP status code and error message.
   */
  override def getBundle(bundleId: BundleId, bundleData: Subscriber[Array[Byte]], configData: Subscriber[Array[Byte]])(implicit cc: ConnectionContext): Future[BundleGetResult] =
    handler.withConnectedRequest(Payload.getBundle(bundleId)) { (responseCode, responseHeader, responseEntity) =>
      import cc.actorMaterializer
      import cc.actorMaterializer.executionContext

      def bundleGet: Future[BundleGetResult] = {

        def bundleFileFromResponse(bundleParts: Seq[(String, Multipart.FormData.BodyPart)]): String =
          bundleParts match {
            case Seq((fileName, bodyPart)) if bodyPart.name == "bundle" =>
              bodyPart.entity.dataBytes
                .map(_.toArray)
                .runWith(Sink.fromSubscriber(bundleData))

              fileName

            case _ =>
              val error = InvalidBundleGetResponseBody("Unable to find bundle file in the response body")

              Source.failed[Array[Byte]](error)
                .alsoTo(Sink.fromSubscriber(configData))
                .runWith(Sink.fromSubscriber(bundleData))

              throw error
          }

        def configFileFromResponse(configParts: Seq[(String, Multipart.FormData.BodyPart)]): Option[String] =
          configParts.find(_._2.name == "configuration") match {
            case Some((fileName, configPart)) =>
              configPart.entity.dataBytes
                .map(_.toArray)
                .runWith(Sink.fromSubscriber(configData))

              Some(fileName)

            case _ =>
              Source.empty[Array[Byte]]
                .runWith(Sink.fromSubscriber(configData))
              None
          }

        for {
          formData <- Unmarshal(responseEntity.withoutSizeLimit()).to[Multipart.FormData]
          bundleAndOthers <- formData.parts
            .map(b => b.filename -> b)
            .collect {
              case (Some(fileName), bodyPart) =>
                fileName -> bodyPart
            }
            .prefixAndTail(1)
            .runWith(Sink.head)

          (bundleParts, otherParts) = bundleAndOthers
          bundleFileName = bundleFileFromResponse(bundleParts)

          configAndOthers <- otherParts.prefixAndTail(1).runWith(Sink.head)
          (configParts, remaining) = configAndOthers
          configFileName = configFileFromResponse(configParts)

          _ <- remaining.map(_._2.entity.dataBytes.runWith(Sink.ignore)).runWith(Sink.ignore)
        } yield BundleGetSuccess(bundleId, bundleFileName, configFileName)
      }

      def bundleGetFailure: Future[BundleGetFailure] = {
        implicit val unmarshaller = PredefinedFromEntityUnmarshallers.stringUnmarshaller
        for {
          httpErrorMessage <- Unmarshal(responseEntity).to[String]
        } yield {
          Source.failed[Array[Byte]](new RuntimeException(s"HTTP Failure when getting bundle - http code [$responseCode] - message [$httpErrorMessage]"))
            .alsoTo(Sink.fromSubscriber(configData))
            .runWith(Sink.fromSubscriber(bundleData))

          BundleGetFailure(responseCode, httpErrorMessage)
        }
      }

      ResponseHandler.withHttpFailure(responseCode)(bundleGet, bundleGetFailure)
    }

  /**
   * @see [[AbstractControlClient.getBundleDescriptor()]]
   *
   * Bundle descriptor is derived from `bundle.conf` and merging it with `bundle.conf` from bundle configuration if supplied.
   * @param bundleId An existing bundle identifier, a shortened version of it (min 7 characters) or
   *                 a non-ambiguous name given to the bundle during loading.
   * @param cc implicit connection context
   * @return The result as a `Future[BundleGetDescriptorResult]`. `BundleGetDescriptorResult` is a sealed trait and can
   *         be either:
   *         - BundleDescriptorGetSuccess if the bundle descriptor retrieval is successful. This object contains the actual bundle descriptor.
   *         - BundleDescriptorGetFailure if http request failed. The object contains the HTTP status code and error message.
   */
  def getBundleDescriptor(bundleId: BundleId)(implicit cc: CC): Future[BundleGetDescriptorResult] = {
    import cc.actorMaterializer.executionContext
    getBundleDescriptorConfig(bundleId)
      .map {
        case BundleGetDescriptorConfigSuccess(config)      => BundleGetDescriptorSuccess(BundleDescriptor.fromConfig(config))
        case BundleGetDescriptorConfigFailure(code, error) => BundleGetDescriptorFailure(code, error)
      }
  }

  /**
   * @see [[AbstractControlClient.getBundleDescriptorConfig()]]
   *
   * Bundle descriptor is derived from `bundle.conf` and merging it with `bundle.conf` from bundle configuration if supplied.
   * @param bundleId An existing bundle identifier, a shortened version of it (min 7 characters) or
   *                 a non-ambiguous name given to the bundle during loading.
   * @param cc implicit connection context
   * @return The result as a `Future[BundleGetDescriptorResult]`. `BundleGetDescriptorResult` is a sealed trait and can
   *         be either:
   *         - BundleDescriptorGetConfigSuccess if the bundle descriptor retrieval is successful. This object contains the actual bundle descriptor.
   *         - BundleDescriptorGetFailure if http request failed. The object contains the HTTP status code and error message.
   */
  def getBundleDescriptorConfig(bundleId: BundleId)(implicit cc: CC): Future[BundleGetDescriptorConfigResult] =
    handler.withConnectedRequest(Payload.getBundleDescriptor(bundleId)) { (responseCode, responseHeader, responseEntity) =>
      import cc.actorMaterializer
      import cc.actorMaterializer.executionContext

      def bundleGetDescriptor: Future[BundleGetDescriptorConfigSuccess] = {
        implicit val unmarshaller = PredefinedFromEntityUnmarshallers.stringUnmarshaller
        for {
          hoconText <- Unmarshal(responseEntity).to[String]
        } yield {
          BundleGetDescriptorConfigSuccess(ConfigFactory.parseString(hoconText).root())
        }
      }

      def bundleGetDescriptorFailure: Future[BundleGetDescriptorConfigFailure] = {
        implicit val unmarshaller = PredefinedFromEntityUnmarshallers.stringUnmarshaller
        for {
          httpErrorMessage <- Unmarshal(responseEntity).to[String]
        } yield {
          BundleGetDescriptorConfigFailure(responseCode, httpErrorMessage)
        }
      }
      ResponseHandler.withHttpFailure(responseCode)(bundleGetDescriptor, bundleGetDescriptorFailure)
    }

  /**
   * @see [[AbstractControlClient.loadBundle()]]
   * @param bundle The file that is the bundle.
   *               The filename is important with its hex digest string and is required to be consistent
   *               with the SHA-256 hash of the bundle’s contents.
   *               Any inconsistency between the hashes will result in the load being rejected.
   * @param config Optional: Similar in form to the bundle, only that is the file that describes the configuration.
   *               Again any inconsistency between the hex digest string in the filename, and the SHA-256 digest
   *               of the actual contents will result in the load being rejected.
   * @param cc implicit connection context
   * @return The result as a Future[BundleRequestResult]. BundleRequestResult is a sealed trait and can be either:
   *         - BundleRequestSuccess if the loading request has been succeeded. This object contains the request and bundle id
   *         - BundleRequestFailure if the loading request has been failed. This object contains the HTTP status code and error message.
   */
  @deprecated("To be replaced with loadBundle with files supplied via reactive stream publishers", since = "1.4.11")
  override def loadBundle(bundle: URI, config: Option[URI] = None)(implicit cc: ConnectionContext): Future[BundleRequestResult] = {
    def filename(uri: URI): String =
      Paths.get(uri.getPath).getFileName.toString

    def filePublisher(uri: URI): Publisher[Array[Byte]] =
      toByteArrayPublisher(FileIO.fromPath(Paths.get(uri.getPath)))

    val tmpDir = new File(System.getProperty("java.io.tmpdir"))

    val bundleConf = extractZipEntry("bundle.conf", bundle, tmpDir).get
    val bundleConfOverlay = config.flatMap(extractZipEntry("bundle.conf", _, tmpDir))

    val bundleConfData = filePublisher(bundleConf)
    val bundleConfOverlayData = bundleConfOverlay.map(filePublisher)
    val bundleData = BundleFile(filename(bundle), filePublisher(bundle))
    val configData = config.map(v => BundleConfigurationFile(filename(v), filePublisher(v)))

    loadBundle(bundleConfData, bundleConfOverlayData, bundleData, configData)
  }

  /**
   * @see [[AbstractControlClient.loadBundle()]]
   * @param bundleConf bundle.conf contained within the `bundle` file.
   * @param bundleConfOverlay bundle.conf override contained within the `config` file.
   * @param bundle The file that is the bundle.
   *               The filename is important with its hex digest string and is required to be consistent
   *               with the SHA-256 hash of the bundle’s contents.
   *               Any inconsistency between the hashes will result in the load being rejected.
   * @param config Similar in form to the bundle, only that is the file that describes the configuration.
   *               Again any inconsistency between the hex digest string in the filename, and the SHA-256 digest
   *               of the actual contents will result in the load being rejected.
   * @param cc implicit connection context
   * @return The result as a Future[BundleRequestResult]. BundleRequestResult is a sealed trait and can be either:
   *         - BundleRequestSuccess if the loading request has been succeeded. This object contains the request and bundle id
   *         - BundleRequestFailure if the loading request has been failed. This object contains the HTTP status code and error message.
   */
  override def loadBundle(bundleConf: Publisher[Array[Byte]], bundleConfOverlay: Option[Publisher[Array[Byte]]], bundle: BundleFile, config: Option[BundleConfigurationFile])(implicit cc: ConnectionContext): Future[BundleRequestResult] = {
    import cc.actorMaterializer
    import cc.context.dispatcher

    def createRequestBody: Future[RequestEntity] = {

      def fileBodyPart(name: String, filename: String, source: Source[ByteString, _]): Multipart.FormData.BodyPart =
        Multipart.FormData.BodyPart(
          name,
          IndefiniteLength(MediaTypes.`application/octet-stream`, source),
          Map("filename" -> filename)
        )

      def toByteStringSource(input: Publisher[Array[Byte]]): Source[ByteString, _] =
        Source.fromPublisher(input).map(ByteString(_))

      val bundleConfBodyPart = fileBodyPart("bundleConf", "bundle.conf", toByteStringSource(bundleConf))
      val bundleConfOverlayBodyPart = bundleConfOverlay.map(overlay => fileBodyPart("bundleConfOverlay", "bundle.conf", toByteStringSource(overlay)))
      val bundleFileBodyPart = fileBodyPart("bundle", bundle.fileName, toByteStringSource(bundle.data))
      val configFileBodyPart = config.map(c => fileBodyPart("configuration", c.fileName, toByteStringSource(c.data)))

      val bodyParts = List(Some(bundleConfBodyPart), bundleConfOverlayBodyPart, Some(bundleFileBodyPart), configFileBodyPart).flatten
      val result = Marshal(Multipart.FormData(Source(bodyParts))).to[RequestEntity]
      result
    }

    handler.withConnectedRequest(Payload.loadBundle, Some(createRequestBody))(handleAsHttpFailure[BundleRequestResult, BundleRequestSuccess, BundleRequestFailure])
  }

  /**
   * @see [[AbstractControlClient.loadBundleComplete()]]
   * @param bundle The file that is the bundle.
   *               The filename is important with its hex digest string and is required to be consistent
   *               with the SHA-256 hash of the bundle’s contents.
   *               Any inconsistency between the hashes will result in the load being rejected.
   * @param config Optional: Similar in form to the bundle, only that is the file that describes the configuration.
   *               Again any inconsistency between the hex digest string in the filename, and the SHA-256 digest
   *               of the actual contents will result in the load being rejected.
   * @param cc implicit connection context
   * @return The result as a Future[BundleRequestResult]. BundleRequestResult is a sealed trait and can be either:
   *         - BundleRequestSuccess if the loading request has been succeeded. This object contains the request and bundle id
   *         - BundleRequestFailure if the loading request has been failed. This object contains the HTTP status code and error message.
   */
  def loadBundleComplete(bundleConf: Publisher[Array[Byte]], bundleConfOverlay: Option[Publisher[Array[Byte]]], bundle: BundleFile, config: Option[BundleConfigurationFile], completeTimeout: FiniteDuration = 30.seconds)(implicit cc: ConnectionContext): Future[BundleRequestResult] = {
    import cc.context
    import cc.context.dispatcher
    import cc.actorMaterializer

    loadBundle(bundleConf, bundleConfOverlay, bundle, config).flatMap {
      case v @ BundleRequestSuccess(_, bundleIdActual) =>
        def isInstalled(bundles: Seq[Bundle]): Boolean =
          bundles.filter(_.bundleId == bundleIdActual).exists(_.bundleInstallations.nonEmpty)

        for {
          bundlesEventsRequest <- handler.createRequest(Payload.bundlesEvents(Set.empty))
          bundlesRequest <- handler.createRequest(Payload.bundlesInfo)
          result <- Source.single(bundlesEventsRequest -> bundlesRequest)
            .via(BundlesConnector.connect(conductrAddress, stopAfter = Some(completeTimeout)))
            .filter(isInstalled)
            .runWith(Sink.head)
            .map(_ => v)
            .recoverWith {
              case BundlesConnector.TimeoutException =>
                Future.failed(BundleRequestTimedOut(s"Timed out waiting for bundle [$bundleIdActual] to be installed"))
            }
        } yield result

      case v: BundleRequestFailure =>
        Future.successful(v)
    }
  }

  /**
   * @see [[AbstractControlClient.runBundle()]]
   * @param bundleId An existing bundle identifier, a shortened version of it (min 7 characters) or
   *                 a non-ambiguous name given to the bundle during loading.
   * @param scale The number of instances of the bundle to start. Defaults to 1.
   * @param affinity Optional: Identifier to other bundle.
   *                 If specified, the current bundle will be run on the same host where
   *                 the specified bundle is currently running.
   * @param cc implicit connection context
   * @return The result as a Future[BundleRequestResult]. BundleRequestResult is a sealed trait and can be either:
   *         - BundleRequestSuccess if the scaling request has been succeeded. This object contains the request and bundle id
   *         - BundleRequestFailure if the scaling request has been failed. This object contains the HTTP status code and error message.
   */
  override def runBundle(bundleId: BundleId, scale: Option[Int] = None, affinity: Option[String] = None)(implicit cc: ConnectionContext): Future[BundleRequestResult] =
    handler.withConnectedRequest(Payload.runBundle(bundleId, scale.getOrElse(DefaultScale), affinity))(handleAsHttpFailure[BundleRequestResult, BundleRequestSuccess, BundleRequestFailure])

  /**
   * @see [[AbstractControlClient.runBundleComplete()]]
   * @param bundleId An existing bundle identifier, a shortened version of it (min 7 characters) or
   *                 a non-ambiguous name given to the bundle during loading.
   * @param scale The number of instances of the bundle to start. Defaults to 1.
   * @param affinity Optional: Identifier to other bundle.
   *                 If specified, the current bundle will be run on the same host where
   *                 the specified bundle is currently running.
   * @param cc implicit connection context
   * @return The result as a Future[BundleRequestResult]. BundleRequestResult is a sealed trait and can be either:
   *         - BundleRequestSuccess if the scaling request has been succeeded. This object contains the request and bundle id
   *         - BundleRequestFailure if the scaling request has been failed. This object contains the HTTP status code and error message.
   */
  def runBundleComplete(bundleId: BundleId, scale: Option[Int] = None, affinity: Option[String] = None, completeTimeout: FiniteDuration = 30.seconds)(implicit cc: ConnectionContext): Future[BundleRequestResult] = {
    import cc.context
    import cc.context.dispatcher
    import cc.actorMaterializer

    runBundle(bundleId, scale, affinity).flatMap {
      case v @ BundleRequestSuccess(_, bundleIdActual) =>
        val requiredScale = scale.getOrElse(1)

        def runningBundlesCount(bundles: Seq[Bundle]): Int =
          bundles
            .filter(_.bundleId == bundleIdActual)
            .map(_.bundleExecutions.count(_.isStarted))
            .sum

        def isDesiredScaleAchieved(bundles: Seq[Bundle]) =
          runningBundlesCount(bundles) >= requiredScale

        for {
          bundlesEventsRequest <- handler.createRequest(Payload.bundlesEvents(Set.empty))
          bundlesRequest <- handler.createRequest(Payload.bundlesInfo)
          result <- Source.single(bundlesEventsRequest -> bundlesRequest)
            .via(BundlesConnector.connect(conductrAddress, stopAfter = Some(completeTimeout)))
            .filter(isDesiredScaleAchieved)
            .runWith(Sink.head)
            .map(_ => v)
            .recoverWith {
              case BundlesConnector.TimeoutException =>
                Future.failed(BundleRequestTimedOut(s"Timed out waiting for bundle [$bundleIdActual] to be installed"))
            }
        } yield result

      case v: BundleRequestFailure =>
        Future.successful(v)
    }
  }

  /**
   * @see [[AbstractControlClient.stopBundle()]]
   * @param bundleId An existing bundle identifier, a shortened version of it (min 7 characters) or
   *                 a non-ambiguous name given to the bundle during loading.
   * @param cc
   * @return The result as a Future[BundleRequestResult]. BundleRequestResult is a sealed trait and can be either:
   *         - BundleRequestSuccess if the stopping request has been succeeded. This object contains the request and bundle id
   *         - BundleRequestFailure if the BundleRequestSuccess request has been failed. This object contains the HTTP status code and error message.
   */
  override def stopBundle(bundleId: BundleId)(implicit cc: ConnectionContext): Future[BundleRequestResult] =
    runBundle(bundleId, Some(0), None)

  /**
   * @see [[AbstractControlClient.unloadBundle()]]
   * @param bundleId An existing bundle identifier, a shortened version of it (min 7 characters) or
   *                 a non-ambiguous name given to the bundle during loading.
   * @param cc implicit connection context
   * @return The result as a Future[BundleUnloadResult]. BundleUnloadResult is a sealed trait and can be either:
   *         - BundleUnloadSuccess if the unloading request has been succeeded. This object contains the request id
   *         - BundleUnloadFailure if the unloading request has been failed. This object contains the HTTP status code and error message.
   */
  override def unloadBundle(bundleId: BundleId)(implicit cc: ConnectionContext): Future[BundleRequestResult] =
    handler.withConnectedRequest(Payload.unloadBundle(bundleId))(handleAsHttpFailure[BundleRequestResult, BundleRequestSuccess, BundleRequestFailure])

  /**
   * Returns a stream of all bundle events. Each event is represented by a [[ServerSentEvent]].
   * Each [[ServerSentEvent]] is transferred in a [[Source]] to easily access a stream of events.
   *
   * @param events The requested events the stream should return. A `Set.empty` is returning every event type.
   * @param cc implicit connection context
   * @return The stream as a [[Source]] wrapped inside a [[scala.concurrent.Future]]
   */
  // TODO: Create an abstract method in [[AbstractControlClient]] to support this method for additional non Akka 2.3 flavors
  def streamBundlesEvents(events: Set[String] = Set.empty)(implicit cc: CC): Future[EventStreamResult] =
    handler.withConnectedRequest(Payload.bundlesEvents(events))(handleAsEventStream)

  /**
   * @see [[AbstractControlClient.getBundleEvents()]]
   * @param bundleId An existing bundle identifier, a shortened version of it (min 7 characters) or
   *                 a non-ambiguous name given to the bundle during loading.
   * @param count The number of events to return. Defaults to 10.
   * @param cc implicit connection context
   * @return The result as a Future[BundleEventsResult]. BundleEventsResult is a sealed trait and can be either:
   *         - BundleEventsSuccess if the request has been succeeded. This object contains the requested events.
   *         - BundleEventsFailure if the request has been failed. This object contains the HTTP status code and error message.
   */
  override def getBundleEvents(bundleId: BundleId, count: Option[Int] = None)(implicit cc: ConnectionContext): Future[BundleEventsResult] =
    handler.withConnectedRequest(Payload.bundleEvents(bundleId, count.getOrElse(DefaultEventsCount)))(handleAsHttpFailure[BundleEventsResult, BundleEventsSuccess, BundleEventsFailure])

  /**
   * @see [[AbstractControlClient.getBundleLogs()]]
   * @param bundleId An existing bundle identifier, a shortened version of it (min 7 characters) or
   *                 a non-ambiguous name given to the bundle during loading.
   * @param count The number of events to return. Defaults to 10.
   * @param cc implicit connection context
   * @return The result as a Future[BundleLogsResult]. BundleEventsResult is a sealed trait and can be either:
   *         - BundleLogsSuccess if the request has been succeeded. This object contains the requested log messages.
   *         - BundleLogsFailure if the request has been failed. This object contains the HTTP status code and error message.
   */
  override def getBundleLogs(bundleId: BundleId, count: Option[Int] = None)(implicit cc: ConnectionContext): Future[BundleLogsResult] =
    handler.withConnectedRequest(Payload.bundleLogs(bundleId, count.getOrElse(DefaultLogsCount)))(handleAsHttpFailure[BundleLogsResult, BundleLogsSuccess, BundleLogsFailure])

  /**
   * @see [[AbstractControlClient.getMembersInfo()]]
   * @param cc implicit connection context
   * @return the current ConductR cluster members.
   */
  override def getMembersInfo()(implicit cc: ConnectionContext): Future[MembersInfoResult] =
    handler.withConnectedRequest(Payload.getMembersInfo)(handleAsHttpFailure[MembersInfoResult, MembersInfoSuccess, MembersInfoFailure])

  /**
   * @see [[AbstractControlClient.getMemberInfo()]]
   * @param address
   * @param cc
   * @return
   */
  override def getMemberInfo(address: URI)(implicit cc: ConnectionContext): Future[MemberInfoResult] =
    handler.withConnectedRequest(Payload.getMemberInfo(address))(handleAsHttpFailure[MemberInfoResult, MemberInfoSuccess, MemberInfoFailure])

  /**
   * Returns a stream of all member events. Each event is represented by a [[de.heikoseeberger.akkasse.ServerSentEvent]].
   * Each [[de.heikoseeberger.akkasse.ServerSentEvent]] is transferred in a [[Source]] to easily access a stream of events.
   *
   * @param cc implicit connection context
   * @return The stream as a [[Source]] wrapped inside a [[scala.concurrent.Future]]
   */
  def streamMembersEvents()(implicit cc: ConnectionContext): Future[EventStreamResult] =
    handler.withConnectedRequest(Payload.membersEvents)(handleAsEventStream)

  /**
   * @see [[com.typesafe.conductr.clientlib.scala.AbstractControlClient.joinMember()]]
   * @param joinTo The uri representing the ConductR cluster member.
   * @param cc implicit connection context
   * @return true if the request has been succeeded.
   *         false if the request has been failed.
   */
  override def joinMember(joinTo: URI)(implicit cc: ConnectionContext): Future[Boolean] = {
    import cc.actorMaterializer.executionContext
    val body = Marshal(FormData(Map("joinTo" -> joinTo.toString))).to[RequestEntity]
    handler.withConnectedRequest(Payload.joinMember, Some(body))(handleAsBoolean)
  }

  /**
   * @see [[com.typesafe.conductr.clientlib.scala.AbstractControlClient.downMember()]]
   * @param address The uri representing the ConductR cluster member.
   * @param cc implicit connection context
   * @return true if the request has been succeeded.
   *         false if the request has been failed.
   */
  override def downMember(address: URI)(implicit cc: ConnectionContext): Future[Boolean] = {
    import cc.actorMaterializer.executionContext
    val body = Marshal(FormData(Map("operation" -> "down"))).to[RequestEntity]
    handler.withConnectedRequest(Payload.downMember(address), Some(body))(handleAsBoolean)
  }

  /**
   * @see [[com.typesafe.conductr.clientlib.scala.AbstractControlClient.leaveMember()]]
   * @param address The uri representing the ConductR cluster member.
   * @param cc implicit connection context
   * @return true if the request has been succeeded.
   *         false if the request has been failed.
   */
  override def leaveMember(address: URI)(implicit cc: ConnectionContext): Future[Boolean] =
    handler.withConnectedRequest(Payload.leaveMember(address))(handleAsBoolean)

  /**
   * Akka 2.3 Response handler object
   */
  object ResponseHandler extends BaseResponseHandler

  /**
   * Akka 2.3 Payload object
   */
  object Payload extends BasePayload(apiVersion) {

    def bundlesEvents(events: Set[String]): HttpPayload = {
      val eventsQueryParam = events.map(event => s"events=$event").mkString("&")
      val query = if (eventsQueryParam.isEmpty) "" else s"?$eventsQueryParam"
      createPayload("GET", s"$Prefix/bundles/events$query")
        .addRequestHeader(`Cache-Control`.name, `no-cache`.toString)
    }

    val membersEvents: HttpPayload = createPayload("GET", s"$Prefix/members/events")
  }

  /**
   * Handle response with custom scala object, e.g. BundleRequestResult, BundleRequestSuccess, BundleRequestFailure
   */
  private def handleAsHttpFailure[T, S <: T, F <: T](code: Int, headers: Map[String, Option[String]], body: ResponseEntity)(implicit cc: ConnectionContext, readsSuccess: Reads[S], readsFailure: Reads[F]): Future[T] = {
    import cc.actorMaterializer
    import cc.actorMaterializer.executionContext

    def onSuccess: Future[S] = Unmarshal(body).to[S]
    def onFailure: Future[F] = toHttpFailure[F](code, body)

    ResponseHandler.withHttpFailure(code)(onSuccess, onFailure)
  }

  private def toHttpFailure[F](code: Int, body: ResponseEntity)(implicit cc: ConnectionContext, readsFailure: Reads[F]): Future[F] = {
    import cc.actorMaterializer
    import cc.actorMaterializer.executionContext

    val errorF = body.contentType match {
      case ContentTypes.`application/json` =>
        Unmarshal(body).to[String]
      case ContentTypes.`text/plain(UTF-8)` =>
        implicit val unmarshaller = PredefinedFromEntityUnmarshallers.stringUnmarshaller
        Unmarshal(body).to[String]
      case unknownContentType =>
        Future.successful(s"ConductR responded with an unknown contentType: $unknownContentType")
    }

    errorF.map(error => Json.obj("code" -> code, "error" -> error).as[F])
  }

  /**
   * Handle response as a `Seq`. In the failure case an empty `Seq` is returned.
   */
  private def handleAsSeq[T](code: Int, headers: Map[String, Option[String]], body: ResponseEntity)(implicit cc: ConnectionContext, reads: Reads[T]): Future[Seq[T]] = {
    import cc.actorMaterializer
    import cc.actorMaterializer.executionContext

    def onSuccess: Future[Seq[T]] =
      Unmarshal(body).to[Seq[T]]
    def onFailure: Future[Seq[T]] =
      Future.successful(Seq.empty)

    ResponseHandler.withSeq(code)(onSuccess, onFailure)
  }

  /**
   * Handle response as a `Boolean`.
   */
  private def handleAsBoolean(code: Int, headers: Map[String, Option[String]], body: ResponseEntity)(implicit cc: ConnectionContext): Future[Boolean] =
    ResponseHandler.withBoolean(code)

  /**
   * Handle response as `EventStreamResult`
   */
  private def handleAsEventStream(code: Int, headers: Map[String, Option[String]], body: ResponseEntity)(implicit cc: ConnectionContext, readsFailure: Reads[EventStreamFailure]): Future[EventStreamResult] = {
    import cc.actorMaterializer
    import cc.actorMaterializer.executionContext

    def onSuccess: Future[EventStreamResult] =
      Unmarshal(body)
        .to[Source[ServerSentEvent, Any]]
        .map(source => EventStreamSuccess(source.mapMaterializedValue(_ => ())))
    def onFailure: Future[EventStreamResult] =
      toHttpFailure[EventStreamFailure](code, body)

    ResponseHandler.withHttpFailure(code)(onSuccess, onFailure)
  }

  private def absolute(uri: URI): URI =
    if (uri.isAbsolute) uri
    else new URI("file", uri.getUserInfo, uri.getHost, uri.getPort, uri.getPath, uri.getQuery, uri.getFragment)

  private def filename(path: String): String =
    path.split('/').lastOption.getOrElse("")

  private def toByteArrayPublisher(input: Source[ByteString, _])(implicit cc: ConnectionContext): Publisher[Array[Byte]] = {
    import cc.actorMaterializer
    input.map(_.toArray).runWith(Sink.asPublisher(fanout = false))
  }

  // TODO: Use Akka stream to extract and read the zip file.
  //       In the current Akka streams version 2.0.1 there is no utility to extract a zip file and iterate over
  //       the files inside of the zip file. Therefore a blocking solution inspired by sbt.IO is used.
  private def extractZipEntry(entryName: String, from: URI, destDir: File): Option[URI] = {
    def stream(in: InputStream, target: File, bufferSize: Int = 8192) = {
      withCloseable(new BufferedOutputStream(new FileOutputStream(target, false))) { out =>
        val buffer = new Array[Byte](bufferSize)
        def read() {
          val byteCount = in.read(buffer)
          if (byteCount >= 0) {
            out.write(buffer, 0, byteCount)
            read()
          }
        }
        read()
      }
    }

    blocking {
      val zipFile = new File(absolute(from).getPath)
      withZipInputStream(new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) { in =>
        Stream
          .continually(in.getNextEntry)
          .takeWhile(_ != null)
          .find(currentEntry => filename(currentEntry.getName) == entryName)
          .map { entry =>
            val target = new File(destDir, entryName)
            stream(in, target)
            target.toURI
          }
      }
    }
  }
}
