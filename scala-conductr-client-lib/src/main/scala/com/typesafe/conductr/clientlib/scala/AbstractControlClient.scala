package com.typesafe.conductr.clientlib.scala

import java.net.{ URLEncoder, URI, URL }
import com.typesafe.conductr.lib.HttpPayload
import com.typesafe.conductr.clientlib.scala.models._
import com.typesafe.conductr.lib.scala.AbstractConnectionContext
import com.typesafe.config.ConfigObject
import org.reactivestreams.{ Subscriber, Publisher }

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Abstract ConductR control client for all projects based on Scala.
 *
 * @param conductrAddress contains the protocol://host:port of the ConductR control server
 */
abstract class AbstractControlClient(conductrAddress: URL) {
  protected type CC <: AbstractConnectionContext

  /**
   * Retrieve information of all bundles.
   *
   * @param cc implicit connection context
   * @return the bundles
   */
  def getBundlesInfo()(implicit cc: CC): Future[Seq[Bundle]]

  /**
   * Retrieve bundle file and bundle configuration file given a particular bundle id.
   *
   * @param bundleId An existing bundle identifier, a shortened version of it (min 7 characters) or
   *                 a non-ambiguous name given to the bundle during loading.
   * @param bundleData A byte array subscriber to which the bundle data will be streamed into.
   * @param configData A byte array subscriber to which the config data will be streamed into.
   * @param cc implicit connection context
   * @return The result as a Future[BundleGetResult]. BundleGetResult is a sealed trait and can be either:
   *         - BundleGetSuccess if the get bundle request has been succeeded. This object contains the bundle id, bundle file, and optionally the config file.
   *         - BundleGetFailure if the get bundle request has been failed. This object contains the HTTP status code and error message.
   */
  def getBundle(bundleId: BundleId, bundleData: Subscriber[Array[Byte]], configData: Subscriber[Array[Byte]])(implicit cc: CC): Future[BundleGetResult]

  /**
   * Obtains a bundle descriptor given a particular bundle id.
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
  def getBundleDescriptor(bundleId: BundleId)(implicit cc: CC): Future[BundleGetDescriptorResult]

  /**
   * Obtains a bundle descriptor given a particular bundle id.
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
  def getBundleDescriptorConfig(bundleId: BundleId)(implicit cc: CC): Future[BundleGetDescriptorResult]

  /**
   * Scale a loaded bundle to a number of instances.
   *
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
  def runBundle(bundleId: BundleId, scale: Option[Int], affinity: Option[String] = None)(implicit cc: CC): Future[BundleRequestResult]

  /**
   * Scale a loaded bundle to a number of instances and returns a future.
   * If `completeWhenScaleAchieved` is set to true, the future will be completed when the requested number of scale is
   * achieved. Otherwise, the future is completed when the HTTP call to be control protocol is completed.
   *
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
  def runBundleComplete(bundleId: BundleId, scale: Option[Int] = None, affinity: Option[String] = None, completeTimeout: FiniteDuration = 30.seconds)(implicit cc: CC): Future[BundleRequestResult]

  /**
   * Stop a running bundle. Requests for already stopped bundles will be send to the ConductR control server as well.
   * In this case ConductR is ignoring the request.
   *
   * @param bundleId An existing bundle identifier, a shortened version of it (min 7 characters) or
   *                 a non-ambiguous name given to the bundle during loading.
   * @return The result as a Future[BundleRequestResult]. BundleRequestResult is a sealed trait and can be either:
   *         - BundleRequestSuccess if the stopping request has been succeeded. This object contains the request and bundle id
   *         - BundleRequestFailure if the BundleRequestSuccess request has been failed. This object contains the HTTP status code and error message.
   */
  def stopBundle(bundleId: BundleId)(implicit cc: CC): Future[BundleRequestResult]

  /**
   * Load a bundle with optional configuration.
   *
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
  def loadBundle(bundle: URI, config: Option[URI] = None)(implicit cc: CC): Future[BundleRequestResult]

  /**
   * Load a bundle with optional bundle conf override and optional configuration.
   *
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
  def loadBundle(bundleConf: Publisher[Array[Byte]], bundleConfOverlay: Option[Publisher[Array[Byte]]], bundle: BundleFile, config: Option[BundleConfigurationFile])(implicit cc: CC): Future[BundleRequestResult]

  /**
   * Load a bundle with optional configuration and returns a future.
   * If completeWhenInstalled is set to true, the future will be completed when the bundle is installed.
   * Otherwise, the future is completed when the HTTP call to be control protocol is completed.
   *
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
  def loadBundleComplete(bundleConf: Publisher[Array[Byte]], bundleConfOverlay: Option[Publisher[Array[Byte]]], bundle: BundleFile, config: Option[BundleConfigurationFile], completeTimeout: FiniteDuration = 30.seconds)(implicit cc: CC): Future[BundleRequestResult]
  /**
   * Unload a bundle from all ConductR instances.
   *
   * @param bundleId An existing bundle identifier, a shortened version of it (min 7 characters) or
   *                 a non-ambiguous name given to the bundle during loading.
   * @param cc implicit connection context
   * @return The result as a Future[BundleRequestResult]. BundleRequestResult is a sealed trait and can be either:
   *         - BundleRequestSuccess if the unloading request has been succeeded. This object contains the request and bundle id
   *         - BundleRequestFailure if the unloading request has been failed. This object contains the HTTP status code and error message.
   */
  def unloadBundle(bundleId: BundleId)(implicit cc: CC): Future[BundleRequestResult]

  /**
   * Retrieve the events of a given bundle. Events with the latest timestamp are going to be returned in a 'tail' like fashion.
   *
   * @param bundleId An existing bundle identifier, a shortened version of it (min 7 characters) or
   *                 a non-ambiguous name given to the bundle during loading.
   * @param count The number of events to return.  Defaults to 10.
   * @param cc implicit connection context
   * @return The result as a Future[BundleEventsResult]. BundleEventsResult is a sealed trait and can be either:
   *         - BundleEventsSuccess if the request has been succeeded. This object contains the requested events.
   *         - BundleEventsFailure if the request has been failed. This object contains the HTTP status code and error message.
   */
  def getBundleEvents(bundleId: BundleId, count: Option[Int])(implicit cc: CC): Future[BundleEventsResult]

  /**
   * Retrieve the log messages of a given bundle. Log messages with the latest timestamp are going to be returned
   * in a 'tail' like fashion.
   *
   * @param bundleId An existing bundle identifier, a shortened version of it (min 7 characters) or
   *                 a non-ambiguous name given to the bundle during loading.
   * @param count The number of events to return.  Defaults to 10.
   * @param cc implicit connection context
   * @return The result as a Future[BundleLogsResult]. BundleEventsResult is a sealed trait and can be either:
   *         - BundleLogsSuccess if the request has been succeeded. This object contains the requested log messages.
   *         - BundleLogsFailure if the request has been failed. This object contains the HTTP status code and error message.
   */
  def getBundleLogs(bundleId: BundleId, count: Option[Int])(implicit cc: CC): Future[BundleLogsResult]

  /**
   * Retrieve the current state of ConductR cluster members.
   *
   * @param cc implicit connection context
   * @return the current ConductR cluster members.
   */
  def getMembersInfo()(implicit cc: CC): Future[MembersInfoResult]

  /**
   * Retrieve the current state of a given ConductR cluster member.
   *
   * @param address The uri representing the ConductR cluster member.
   * @param cc implicit connection context
   * @return The result as a Future[MemberInfoResult]. MemberInfoResult is a sealed trait and can be either:
   *         - MemberInfoSuccess if the request has been succeeded. This object contains the requested ConductR cluster member.
   *         - MemberInfoFailure if the request has been failed. This object contains the HTTP status code and error message.
   */
  def getMemberInfo(address: URI)(implicit cc: CC): Future[MemberInfoResult]

  /**
   * The current ConductR instance joins the given ConductR cluster.
   * This method can be only used inside ConductR.
   *
   * @param joinTo The uri representing the ConductR cluster member.
   * @param cc implicit connection context
   * @return true if the request has been succeeded.
   *         false if the request has been failed.
   */
  def joinMember(joinTo: URI)(implicit cc: CC): Future[Boolean]

  /**
   * The current ConductR instance is downed for the given ConductR cluster.
   * This method can be only used inside ConductR.
   *
   * @param address The uri representing the ConductR cluster member.
   * @param cc implicit connection context
   * @return true if the request has been succeeded.
   *         false if the request has been failed.
   */
  def downMember(address: URI)(implicit cc: CC): Future[Boolean]

  /**
   * The current ConductR instance leaves the given ConductR cluster.
   * This method can be only used inside ConductR.
   *
   * @param address The uri representing the ConductR cluster member.
   * @param cc implicit connection context
   * @return true if the request has been succeeded.
   *         false if the request has been failed.
   */
  def leaveMember(address: URI)(implicit cc: CC): Future[Boolean]

  /**
   * The BasePayload containing helper methods
   * to create [[com.typesafe.conductr.HttpPayload]] objects for all given ControlClient endpoints.
   *
   * @param apiVersion The version of ConductR's REST API
   */
  protected class BasePayload(apiVersion: ApiVersion.Value) {
    final val Prefix = s"$conductrAddress/v$apiVersion"

    def createPayload(method: String, url: String): HttpPayload =
      new HttpPayload(new URL(url), method)

    // format: OFF
    // Bundles
    val bundlesInfo                                   = createPayload("GET",    s"$Prefix/bundles")
    def getBundle(bundleId: BundleId)                 = createPayload("GET",    s"$Prefix/bundles/$bundleId").addRequestHeader("Accept", "multipart/form-data")
    def getBundleDescriptor(bundleId: BundleId)       = createPayload("GET",    s"$Prefix/bundles/$bundleId").addRequestHeader("Accept", "application/hocon")
    def loadBundle                                    = createPayload("POST",   s"$Prefix/bundles")
    def runBundle(bundleId: BundleId, scale: Int, affinity: Option[String]) =
      createPayload("PUT",    s"$Prefix/bundles/$bundleId?scale=$scale${affinity.fold("")("&affinity=" + _)}")

    def unloadBundle(bundleId: BundleId)              = createPayload("DELETE", s"$Prefix/bundles/$bundleId")
    def bundleEvents(bundleId: BundleId, count: Int)  = createPayload("GET",    s"$Prefix/bundles/$bundleId/events?count=$count")
    def bundleLogs(bundleId: BundleId, count: Int)    = createPayload("GET",    s"$Prefix/bundles/$bundleId/logs?count=$count")

    // Members
    val getMembersInfo                                = createPayload("GET",    s"$Prefix/members")
    def getMemberInfo(address: URI)                   = createPayload("GET",    s"$Prefix/members/${encodeURI(address)}")
    val joinMember                                    = createPayload("POST",   s"$Prefix/members")
    def downMember(address: URI)                      = createPayload("PUT",    s"$Prefix/members/${encodeURI(address)}")
    def leaveMember(address: URI)                     = createPayload("DELETE", s"$Prefix/members/${encodeURI(address)}")
    val getMembersEvents                              = createPayload("GET",    s"$Prefix/members/events")
    // format: ON

    private def encodeURI(uri: URI): String =
      URLEncoder.encode(uri.toString, "Utf8")
  }

  /**
   * The BaseResponseHandler provides common methods to handle an HTTP responses from the ConductR control server.
   */
  protected trait BaseResponseHandler {
    import HttpStatusCodes._

    def withSeq[T](code: Int)(successHandler: => Future[Seq[T]], failureHandler: => Future[Seq[T]]): Future[Seq[T]] =
      if (isSuccess(code)) successHandler
      else failureHandler

    def withHttpFailure[T](code: Int)(successHandler: => Future[T], failureHandler: => Future[T]): Future[T] =
      if (isSuccess(code)) successHandler
      else failureHandler

    def withBoolean(code: Int): Future[Boolean] =
      if (isSuccess(code)) Future.successful(true)
      else Future.successful(false)
  }
}
