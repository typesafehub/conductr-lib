package com.typesafe.conductr.bundlelib.lagom.scaladsl

import com.typesafe.conductr.lib.AkkaUnitTest

class EnvSpecWithEnv extends AkkaUnitTest("EnvSpecWithEnvForHost") {

  val config = Env.asConfig

  "The Env functionality in the library" should {
    "initialize the env like expected" in {
      config.getString("lagom.defaults.persistence.read-side.cassandra.keyspace") shouldBe "my_project"
      config.getString("cassandra-snapshot-store.defaults.keyspace") shouldBe "my_project"
      config.getString("cassandra-journal.defaults.keyspace") shouldBe "my_project"
    }
  }
}
