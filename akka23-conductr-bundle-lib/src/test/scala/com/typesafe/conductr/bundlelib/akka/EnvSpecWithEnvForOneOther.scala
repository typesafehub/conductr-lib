package com.typesafe.conductr.bundlelib.akka

import com.typesafe.conductr.lib.AkkaUnitTest
import com.typesafe.config.ConfigException.Missing

class EnvSpecWithEnvForOneOther extends AkkaUnitTest("EnvSpecWithEnvForOthers") {

  val systemName = Env.mkSystemName("MyApp1")
  val config = Env.asConfig(systemName)

  "The Env functionality in the library" should {
    "return seed properties when running with one other seed node" in {
      config.getString("akka.cluster.seed-nodes.0") shouldBe "akka.udp://some-system-v1@10.0.1.11:10001"
      intercept[Missing](config.getString("akka.cluster.seed-nodes.1"))
    }
  }
}
