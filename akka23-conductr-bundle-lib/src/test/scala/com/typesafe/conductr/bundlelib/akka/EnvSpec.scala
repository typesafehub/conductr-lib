package com.typesafe.conductr.bundlelib.akka

import com.typesafe.conductr.lib.AkkaUnitTest
import com.typesafe.config.ConfigException.Missing

class EnvSpec extends AkkaUnitTest("EnvSpec") {

  val systemName = Env.mkSystemName("MyApp1")
  val config = Env.asConfig(systemName)

  "The Env functionality in the library" should {
    "provide the fallback system name" in {
      systemName shouldBe "MyApp1"
    }

    "return no seed properties when running in development mode" in {
      intercept[Missing](config.getString("akka.cluster.seed-nodes.0"))
      intercept[Missing](config.getString("akka.remote.netty.tcp.hostname"))
      intercept[Missing](config.getString("akka.remote.netty.tcp.port"))
    }
  }
}
