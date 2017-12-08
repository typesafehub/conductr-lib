package com.typesafe.conductr.bundlelib.play.api

import com.typesafe.conductr.lib.AkkaUnitTest
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

class EnvSpecWithEnv extends AkkaUnitTest("EnvSpecWithEnvForHost") {

  val config = Env.asConfig

  "The Env functionality in the library" should {
    "initialize the env like expected" in {
      config.getString("play.akka.actor-system") shouldBe "somesys-v1"
      config.getStringList("play.filters.hosts.allowed").asScala shouldBe List("1.1.1.1")
    }

    "merged the allowed hosts config" in {
      val existing = ConfigFactory.parseString(
        """
          |play.filters.hosts.allowed = ["a.com", "b.com"]
        """.stripMargin
      )

      val result = Env.asConfig("my-system", existing)

      result.getString("play.akka.actor-system") shouldBe "my-system"
      result.getStringList("play.filters.hosts.allowed").asScala shouldBe List("a.com", "b.com", "1.1.1.1")
    }
  }
}
