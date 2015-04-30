package com.typesafe.conductr

import akka.actor.ActorSystem
import akka.testkit.TestKitExtension
import akka.util.Timeout
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.BeforeAndAfterAll

object AkkaUnitTest {

  val config: Config = {
    val extra =
      ConfigFactory.parseString("""|akka {
                                   |  actor.debug.fsm = off
                                   |
                                   |  loglevel = debug
                                   |
                                   |  remote {
                                   |    enabled-transports          = [akka.remote.netty.tcp]
                                   |    log-remote-lifecycle-events = off
                                   |
                                   |    netty.tcp {
                                   |      hostname = "127.0.0.1"
                                   |      port     = 0
                                   |    }
                                   |  }
                                   |
                                   |  log-dead-letters                 = on
                                   |  log-dead-letters-during-shutdown = on
                                   |
                                   |  jvm-exit-on-fatal-error = off
                                   |
                                   |  test.timefactor = 1
                                   |}
                                   |""".stripMargin)
    ConfigFactory.defaultOverrides().withFallback(extra).withFallback(ConfigFactory.load())
  }
}

/**
 * Akka unit tests for Typesafe ConductR.
 */
trait AkkaUnitTestLike {

  protected def name: String

  private val config: Config =
    AkkaUnitTest.config

  protected def startSystem(cfg: Config): ActorSystem =
    ActorSystem(s"$name-spec", cfg.withFallback(config))

  protected def testTimeout(system: ActorSystem): Timeout =
    TestKitExtension(system).DefaultTimeout

  protected def shutdownSystem(system: ActorSystem): Unit = {
    system.shutdown()
    system.awaitTermination()
  }
}

/**
 * Akka unit tests for Typesafe ConductR.
 */
abstract class AkkaUnitTest(override val name: String = "default", configString: String = "")
    extends UnitTestLike
    with AkkaUnitTestLike
    with BeforeAndAfterAll {

  protected implicit val system =
    startSystem(ConfigFactory.parseString(configString))

  protected implicit val timeout: Timeout =
    TestKitExtension(system).DefaultTimeout

  override protected def afterAll(): Unit =
    shutdownSystem(system)
}
