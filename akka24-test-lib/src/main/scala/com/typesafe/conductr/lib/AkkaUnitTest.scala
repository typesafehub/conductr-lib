package com.typesafe.conductr.lib

import _root_.akka.actor.ActorSystem
import _root_.akka.testkit.TestKitExtension
import _root_.akka.util.Timeout
import _root_.akka.http.scaladsl.testkit.RouteTestTimeout
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest._
import _root_.scala.concurrent.duration._
import _root_.scala.concurrent.Await

object AkkaUnitTest {

  val config: Config = {
    val extra =
      ConfigFactory.parseString("""|akka {
                                   |  actor.debug.fsm = off
                                   |
                                   |  loggers = ["akka.event.Logging$DefaultLogger"]
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
                                   |
                                   |  diagnostics {
                                   |    recorder.enabled = off
                                   |    checker.enabled  = off
                                   |  }
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

  protected def config(testData: Option[TestData]): Config =
    AkkaUnitTest.config

  protected def startSystem(testData: Option[TestData]): ActorSystem =
    ActorSystem(s"$name-spec", config(testData))

  protected def testTimeout(system: ActorSystem): Timeout =
    TestKitExtension(system).DefaultTimeout

  protected implicit def routeTestTimeout: RouteTestTimeout =
    RouteTestTimeout(2.seconds)

  protected def shutdownSystem(system: ActorSystem): Unit =
    Await.result(system.terminate(), 10.seconds)
}

/**
 * Akka unit tests for Typesafe ConductR. The actor system along with
 * its configuration is established just once. Correspondingly the
 * actor system is terminated at the end of all of the tests.
 *
 * Note then that the startSystem and config methods (for example)
 * are passed no test data - there is no test data representing the
 * suite as a whole.
 */
abstract class AkkaUnitTest(override val name: String = "default")
    extends UnitTestLike
    with AkkaUnitTestLike
    with BeforeAndAfterAll {

  protected implicit val system =
    startSystem(None)

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
abstract class AkkaUnitTestWithFixture(override val name: String = "default")
    extends fixture.WordSpec
    with Matchers
    with AkkaUnitTestLike {

  case class FixtureParam(system: ActorSystem, timeout: Timeout)

  override protected def withFixture(test: OneArgTest): Outcome = {
    val system = startSystem(Some(test))
    try {
      val timeout = testTimeout(system)

      super.withFixture(test.toNoArgTest(FixtureParam(system, timeout)))
    } finally
      shutdownSystem(system)
  }
}
