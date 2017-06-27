package com.typesafe.conductr.bundlelib.play.api

import com.typesafe.conductr.lib.AkkaUnitTest

import scala.collection.JavaConverters._

class EnvSpecWithEnv extends AkkaUnitTest("EnvSpecWithEnvForHost") {

  val config = Env.asConfig

  "The Env functionality in the library" should {
    "initialize the env like expected" in {
      config.getString("play.akka.actor-system") shouldBe "somesys-v1"
      config.getStringList("play.filters.hosts.allowed").asScala shouldBe List("1.1.1.1")
    }
  }
}
