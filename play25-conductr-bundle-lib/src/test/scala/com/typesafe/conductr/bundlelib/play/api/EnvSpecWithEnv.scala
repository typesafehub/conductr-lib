package com.typesafe.conductr.bundlelib.play.api

import com.typesafe.conductr.lib.AkkaUnitTest

class EnvSpecWithEnv extends AkkaUnitTest("EnvSpecWithEnvForHost") {

  val config = Env.asConfig

  "The Env functionality in the library" should {
    "initialize the env like expected" in {
      config.getString("http.address") shouldBe "127.0.0.1"
      config.getString("http.port") shouldBe "9000"
      config.getString("play.akka.actor-system") shouldBe "somesys-v1"
    }
  }
}
