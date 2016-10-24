package com.typesafe.conductr.clientlib.akka

import com.typesafe.conductr.clientlib.scala.models.Bundle
import com.typesafe.conductr.lib.UnitTest
import play.api.libs.json.Json

class JsonMarshallingSpec extends UnitTest {
  "unmarshalling bundle" should {
    "result in correct json when bundle is declared with endpoints having service URI and request ACLs" in {
      import JsonMarshalling._
      import TestData._

      val result = Json.parse(BundleWithServicesAndRequestAclJson).as[Bundle]
      result shouldBe BundleWithServicesAndRequestAcl
    }
  }
}
