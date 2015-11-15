package com.typesafe.conductr

import akka.actor.ActorSystem
import akka.testkit.TestKitExtension
import akka.util.Timeout
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest._
import scala.util.Random

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

  protected def startSystem(cfg: Config): ActorSystem = {
    val randomNr = Random.nextInt(999)
    ActorSystem(s"$name-spec-$randomNr", cfg.withFallback(config))
  }

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

  protected implicit val timeout: Timeout = {
    val ext = TestKitExtension(system)
    Timeout(ext.DefaultTimeout.duration * ext.TestTimeFactor.toLong)
  }

  override protected def afterAll(): Unit =
    shutdownSystem(system)
}

/**
 * An isolating test for Akka provides a new Akka system for each test run.
 */
abstract class IsolatingAkkaUnitTest(override val name: String = "default", configString: String = "")
    extends fixture.WordSpec
    with Matchers
    with AkkaUnitTestLike
    with Retries {

  case class FixtureParam(system: ActorSystem, timeout: Timeout)

  override protected def withFixture(test: OneArgTest): Outcome = {
    val system = startSystem(ConfigFactory.parseString(configString))
    try {
      val timeout = testTimeout(system)

      if (isRetryable(test))
        withRetry(withFixture(test.toNoArgTest(FixtureParam(system, timeout))))
      else
        withFixture(test.toNoArgTest(FixtureParam(system, timeout)))
    } finally
      shutdownSystem(system)
  }
}
