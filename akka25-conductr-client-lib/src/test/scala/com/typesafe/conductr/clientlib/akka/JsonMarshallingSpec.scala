package com.typesafe.conductr.clientlib.akka

import com.typesafe.conductr.clientlib.scala.models.Bundle
import com.typesafe.conductr.lib.UnitTest
import play.api.libs.json.Json

class JsonMarshallingSpec extends UnitTest {
  "unmarshalling bundle" should {
    import JsonMarshalling._
    import TestData._

    Seq(
      "bundle is declared with endpoints having service URI and request ACLs" -> BundleWithServicesAndRequestAclJson -> BundleWithServicesAndRequestAcl,
      "bundle is declared with tags" -> BundleWithTagsJson -> BundleWithTags
    ).foreach {
        case ((scenario, json), expectedBundle) =>
          s"result in correct json when $scenario" in {
            val result = Json.parse(json).as[Bundle]
            result shouldBe expectedBundle
          }
      }
  }
}
