package com.typesafe.conductr.bundlelib.akka

import com.typesafe.conductr.lib.AkkaUnitTest
import com.typesafe.config.ConfigException.Missing

class EnvSpecWithEnvForOthers extends AkkaUnitTest("EnvSpecWithEnvForOthers") {

  val systemName = Env.mkSystemName("MyApp1")
  val config = Env.asConfig(systemName)

  "The Env functionality in the library" should {
    "return seed properties when running with other seed nodes" in {
      config.getString("akka.cluster.seed-nodes.0") shouldBe "akka.udp://some-system-v1@10.0.1.11:10001"
      config.getString("akka.cluster.seed-nodes.1") shouldBe "akka.tcp://some-system-v1@10.0.1.12:10000"
      intercept[Missing](config.getString("akka.cluster.seed-nodes.2"))
    }
  }
}
