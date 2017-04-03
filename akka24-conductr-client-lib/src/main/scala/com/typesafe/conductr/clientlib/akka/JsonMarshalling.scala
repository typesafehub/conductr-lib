package com.typesafe.conductr.clientlib.akka

import java.net.URI
import java.text.SimpleDateFormat
import java.util.{ TimeZone, Date, UUID }

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.unmarshalling._
import akka.stream.ActorMaterializer
import com.typesafe.conductr.clientlib.akka.models.EventStreamFailure
import com.typesafe.conductr.clientlib.scala.models._
import com.typesafe.conductr.lib.scala.ConductrTypeOps
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.json.Reads._
import scala.collection.immutable.SortedSet
import scala.concurrent.ExecutionContext
import scala.util.Try

object JsonMarshalling {

  import ConductrTypeOps._

  /** Unmarshaller depending on a Play JSON `Reads`, composed with a standard string unmarshaller for application/json. */
  implicit def feum[A](implicit reads: Reads[A], materializer: ActorMaterializer, ec: ExecutionContext): FromEntityUnmarshaller[A] =
    PredefinedFromEntityUnmarshallers.stringUnmarshaller.forContentTypes(MediaTypes.`application/json`).map { s =>
      reads.reads(Json.parse(s)) match {
        case JsSuccess(o, _) => o
        case JsError(errors) =>
          throw new IllegalArgumentException(s"Json can not be converted to an object. Json: $s, Errors: $errors")
      }
    }

  /** Marshaller depending on a Play JSON `Writes`, composed with a standard string marshaller for application/json. */
  implicit def tem[A](implicit writer: Writes[A]): ToEntityMarshaller[A] = {
    val stringMarshaller = PredefinedToEntityMarshallers.stringMarshaller(MediaTypes.`application/json`)
    stringMarshaller.compose(a => writer.writes(a).toString)
  }

  /** Implicit Play JSON formats of model classes **/
  implicit val byteArrayFormat: Format[Array[Byte]] = {
    val reads = Reads {
      case JsString(value) => JsSuccess(hexStringToByteArray(value))
      case _               => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsstring"))))
    }
    val writes: Writes[Array[Byte]] = Writes {
      o: Array[Byte] => JsString(o.toHex)
    }
    Format(reads, writes)
  }
  implicit val uriFormat: Format[URI] = {
    val reads: Reads[URI] = Reads {
      case JsString(s) => JsSuccess(new URI(s))
      case _           => JsError("URI could not be converted to java.net.URI object.")
    }
    val writes: Writes[URI] = Writes {
      uri: URI => JsString(uri.toString)
    }
    Format(reads, writes)
  }
  implicit val uuidFormat: Format[UUID] = {
    val reads: Reads[UUID] = Reads {
      case JsString(s) => JsSuccess(UUID.fromString(s))
      case _           => JsError("UUID could not be converted to java.util.UUID object.")
    }
    val writes: Writes[UUID] = Writes {
      uuid: UUID => JsString(uuid.toString)
    }
    Format(reads, writes)
  }
  implicit val dateFormat: Format[Date] = {
    val isoDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    isoDateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"))
    val reads: Reads[Date] = Reads {
      case JsString(s) =>
        Try {
          JsSuccess(isoDateFormatter.parse(s))
        }.getOrElse(JsError(s"Date $s could not be parsed to java.util.Date object"))
      case _ => JsError("Date $s could not be converted to java.util.Date object.")
    }
    val writes: Writes[Date] = Writes {
      date: Date => JsString(isoDateFormatter.format(date))
    }
    Format(reads, writes)
  }

  implicit object TcpFamilyRequestMappingsFormat extends Format[TcpFamilyRequestMappings] {
    override def writes(o: TcpFamilyRequestMappings): JsValue =
      throw new UnsupportedOperationException()

    override def reads(json: JsValue): JsResult[TcpFamilyRequestMappings] =
      for {
        ports <- (json \ "requests").validate[Seq[Int]]
      } yield TcpFamilyRequestMappings(ports.map(TcpRequestMapping))
  }

  implicit object UdpFamilyRequestMappingsFormat extends Format[UdpFamilyRequestMappings] {
    override def writes(o: UdpFamilyRequestMappings): JsValue =
      throw new UnsupportedOperationException()

    override def reads(json: JsValue): JsResult[UdpFamilyRequestMappings] =
      for {
        ports <- (json \ "requests").validate[Seq[Int]]
      } yield UdpFamilyRequestMappings(ports.map(UdpRequestMapping))
  }

  implicit val httpPathFormat = Json.format[HttpRequestMapping.Path]
  implicit val httpPathBegFormat = Json.format[HttpRequestMapping.PathBeg]
  implicit val httpPathRegexFormat = Json.format[HttpRequestMapping.PathRegex]

  implicit object HttpRequestMappingFormat extends Format[HttpRequestMapping] {
    override def writes(o: HttpRequestMapping): JsValue =
      throw new UnsupportedOperationException()

    override def reads(json: JsValue): JsResult[HttpRequestMapping] =
      json.validate[HttpRequestMapping.Path]
        .orElse(json.validate[HttpRequestMapping.PathBeg])
        .orElse(json.validate[HttpRequestMapping.PathRegex])
  }

  implicit object HttpFamilyRequestMappingsFormat extends Format[HttpFamilyRequestMappings] {
    override def writes(o: HttpFamilyRequestMappings): JsValue =
      throw new UnsupportedOperationException()

    override def reads(json: JsValue): JsResult[HttpFamilyRequestMappings] =
      for {
        requestMappings <- (json \ "requests").validate[Seq[HttpRequestMapping]]
      } yield HttpFamilyRequestMappings(requestMappings)
  }

  implicit object ProtocolFamilyRequestMappingsFormat extends Format[Seq[ProtocolFamilyRequestMappings]] {
    override def writes(o: Seq[ProtocolFamilyRequestMappings]): JsValue =
      throw new UnsupportedOperationException()

    override def reads(json: JsValue): JsResult[Seq[ProtocolFamilyRequestMappings]] =
      for {
        httpRequestMappings <- (json \ "http").validateOpt[HttpFamilyRequestMappings]
        tcpRequestMappings <- (json \ "tcp").validateOpt[TcpFamilyRequestMappings]
        udpRequestMappings <- (json \ "udp").validateOpt[UdpFamilyRequestMappings]
      } yield {
        Seq(httpRequestMappings, tcpRequestMappings, udpRequestMappings)
          .collect {
            case Some(value) => value
          }
      }
  }

  implicit object RequestAclFormat extends Format[RequestAcl] {
    override def writes(requestAcl: RequestAcl): JsValue =
      throw new UnsupportedOperationException()

    override def reads(json: JsValue): JsResult[RequestAcl] =
      for {
        jsonValues <- json.validate[Seq[ProtocolFamilyRequestMappings]]
      } yield RequestAcl(jsonValues)
  }

  implicit object BundleConfigEndpointFormat extends Format[BundleConfigEndpoint] {
    override def writes(o: BundleConfigEndpoint): JsValue =
      throw new UnsupportedOperationException()

    override def reads(json: JsValue): JsResult[BundleConfigEndpoint] =
      for {
        bindProtocol <- (json \ "bindProtocol").validate[String]
        serviceName <- (json \ "serviceName").validateOpt[String]
        services <- (json \ "services").validateOpt[Set[URI]]
        acls <- (json \ "acls").validateOpt[Seq[RequestAcl]]
      } yield BundleConfigEndpoint(
        bindProtocol,
        serviceName,
        services.getOrElse(Set.empty),
        acls.getOrElse(Seq.empty)
      )
  }

  implicit object BundleAttributesFormat extends Format[BundleAttributes] {
    override def writes(o: BundleAttributes): JsValue =
      throw new UnsupportedOperationException()

    override def reads(json: JsValue): JsResult[BundleAttributes] =
      for {
        system <- (json \ "system").validate[String]
        nrOfCpus <- (json \ "nrOfCpus").validate[Double]
        memory <- (json \ "memory").validate[Long]
        diskSpace <- (json \ "diskSpace").validate[Long]
        roles <- (json \ "roles").validate[SortedSet[String]]
        bundleName <- (json \ "bundleName").validate[String]
        systemVersion <- (json \ "systemVersion").validate[String]
        compatibilityVersion <- (json \ "compatibilityVersion").validate[String]
        tags <- (json \ "tags").validateOpt[Seq[String]]
      } yield BundleAttributes(
        system,
        nrOfCpus,
        memory,
        diskSpace,
        roles,
        bundleName,
        systemVersion,
        compatibilityVersion,
        tags.getOrElse(Seq.empty)
      )
  }

  // format: OFF
  implicit val uniqueAddressFormat: Format[UniqueAddress]                 = Json.format
  implicit val bundleExecEndpointFormat: Format[BundleExecutionEndpoint]  = Json.format
  implicit val bundleInstallationFormat: Format[BundleInstallation]       = Json.format
  implicit val bundleExecutionFormat: Format[BundleExecution]             = Json.format
  implicit val bundleConfigFormat: Format[BundleConfig]                   = Json.format
  implicit val bundleScaleFormat: Format[BundleScale]                     = Json.format
  implicit val bundleFormat: Format[Bundle]                               = Json.format
  implicit val bundleEventFormat: Format[BundleEvent]                     = Json.format
  implicit val bundleLogFormat: Format[BundleLog]                         = Json.format
  implicit val bundleRequestSuccessFormat: Format[BundleRequestSuccess]   = Json.format
  implicit val bundleRequestFailureFormat: Format[BundleRequestFailure]   = Json.format
  implicit val bundleUnloadSuccessFormat: Format[BundleUnloadSuccess]     = Json.format
  implicit val bundleUnloadFailureFormat: Format[BundleUnloadFailure]     = Json.format
  implicit val bundleLogsFailureFormat: Format[BundleLogsFailure]         = Json.format
  implicit val bundleEventsFailureFormat: Format[BundleEventsFailure]     = Json.format
  implicit val unreachableMemberFormat: Format[UnreachableMember]         = Json.format
  implicit val memberFormat: Format[Member]                               = Json.format
  implicit val memberInfoFailureFormat: Format[MemberInfoFailure]         = Json.format
  implicit val membersInfoSuccessFormat: Format[MembersInfoSuccess]       = Json.format
  implicit val membersInfoFailureFormat: Format[MembersInfoFailure]       = Json.format
  implicit val membersEventsFailureFormat: Format[EventStreamFailure]     = Json.format
  // format: ON

  implicit val bundleLogsSuccessFormat: Format[BundleLogsSuccess] = {
    val errorMsg = "Bundle logs could not be converted to BundleLogsSuccess"
    val reads: Reads[BundleLogsSuccess] = Reads {
      case array: JsArray => array.validate[Seq[BundleLog]].fold(
        errors => JsError(s"$errorMsg: $errors"),
        logs => JsSuccess(BundleLogsSuccess(logs))
      )
      case other => JsError(s"$errorMsg: $other")
    }
    val writes: Writes[BundleLogsSuccess] = Writes {
      o: BundleLogsSuccess => Json.toJson(o.logs)
    }
    Format(reads, writes)
  }

  implicit val bundleEventsSuccessFormat: Format[BundleEventsSuccess] = {
    val errorMsg = "Bundle events could not be converted to BundleEventsSuccess"
    val reads: Reads[BundleEventsSuccess] = Reads {
      case array: JsArray => array.validate[Seq[BundleEvent]].fold(
        errors => JsError(s"$errorMsg: $errors"),
        events => JsSuccess(BundleEventsSuccess(events))
      )
      case other => JsError(s"$errorMsg: $other")
    }
    val writes: Writes[BundleEventsSuccess] = Writes {
      o: BundleEventsSuccess => Json.toJson(o.events)
    }
    Format(reads, writes)
  }

  /**
   * Helper object to convert MemberInfoSuccess from / to JSON. Necessary because the name of the keys in JSON
   * are not equivalent to the ones in MemberInfoSuccess
   */
  private object MemberResponse { implicit val format: Format[MemberResponse] = Json.format }
  private final case class MemberResponse(
    node: UniqueAddress,
    status: String,
    roles: Set[String],
    isUnreachableFrom: Seq[UniqueAddress],
    detectedUnreachable: Seq[URI]
  )

  implicit val memberInfoSuccessFormat: Format[MemberInfoSuccess] = {
    val errorMsg = "Member information could not be converted to MemberInfoSuccess"
    val reads: Reads[MemberInfoSuccess] = Reads {
      case json: JsObject =>
        json.validate[MemberResponse] match {
          case JsSuccess(member, _) =>
            JsSuccess(
              MemberInfoSuccess(
                member = Member(member.node, member.status, member.roles),
                isUnreachableFrom = member.isUnreachableFrom,
                detectedUnreachable = member.detectedUnreachable
              )
            )
          case JsError(errors) => JsError(s"$errorMsg: $errors")
        }
      case other => JsError(s"$errorMsg: $other")
    }
    val writes: Writes[MemberInfoSuccess] = Writes {
      o: MemberInfoSuccess =>
        val memberResponse = MemberResponse(
          node = o.member.node,
          status = o.member.status,
          roles = o.member.roles,
          isUnreachableFrom = o.isUnreachableFrom,
          detectedUnreachable = o.detectedUnreachable
        )
        Json.toJson(memberResponse)
    }
    Format(reads, writes)
  }
}
