package com.typesafe.conductr.bundlelib.akka

import com.typesafe.conductr.lib.AkkaUnitTest

class EnvSpecWithEnvForHost extends AkkaUnitTest("EnvSpecWithEnvForHost") {

  val systemName = Env.mkSystemName("MyApp1")
  val config = Env.asConfig(systemName)

  "The Env functionality in the library" should {
    "return seed properties when running with no other seed nodes" in {
      config.getString("akka.cluster.seed-nodes.0") shouldBe "akka.tcp://some-system-v1@10.0.1.10:10000"
      config.getString("akka.remote.netty.tcp.hostname") shouldBe "10.0.1.10"
      config.getInt("akka.remote.netty.tcp.port") shouldBe 10000
    }
  }
}
