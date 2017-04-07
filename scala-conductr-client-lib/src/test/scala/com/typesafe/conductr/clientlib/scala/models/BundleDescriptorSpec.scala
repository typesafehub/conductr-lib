package com.typesafe.conductr.clientlib.scala.models

import com.typesafe.config.ConfigFactory
import org.scalatest.{ Matchers, WordSpec }

import scala.collection.immutable.Seq

object BundleDescriptorSpec {
  object WithoutTagsDashed {
    val hocon =
      """
        |compatibility-version="1"
        |components {
        |  eslite {
        |    description = eslite
        |    endpoints {
        |      akka-remote {
        |        bind-port = 0
        |        bind-protocol = tcp
        |        services = []
        |      }
        |      es {
        |        bind-port = 0
        |        bind-protocol = http
        |        service-name = elastic-search
        |        services = []
        |      }
        |    }
        |    file-system-type = universal
        |    start-command = [
        |      "eslite/bin/eslite",
        |      "-J-Xms134217728",
        |      "-J-Xmx134217728",
        |      "-Dhttp.address=$ES_BIND_IP",
        |      "-Dhttp.port=$ES_BIND_PORT",
        |      "-Dplay.crypto.secret=65736c697465"
        |    ]
        |  }
        |}
        |disk-space = 200000000
        |memory = 402653184
        |name = eslite
        |nr-of-cpus = 0.1
        |roles = [elasticsearch]
        |system = eslite
        |system-version = "1"
        |version = "1"
      """.stripMargin

    val bundleDescriptor = BundleDescriptor(
      version = "1",
      system = "eslite",
      systemVersion = "1",
      nrOfCpus = 0.1,
      memory = 402653184L,
      diskSpace = 200000000L,
      roles = Seq("elasticsearch"),
      bundleName = "eslite",
      compatibilityVersion = "1",
      tags = Seq.empty,
      annotations = None,
      components = Map(
        "eslite" -> new BundleDescriptor.Component(
          "eslite",
          BundleDescriptor.Component.FileSystemType.Universal,
          Seq(
            "eslite/bin/eslite",
            "-J-Xms134217728",
            "-J-Xmx134217728",
            "-Dhttp.address=$ES_BIND_IP",
            "-Dhttp.port=$ES_BIND_PORT",
            "-Dplay.crypto.secret=65736c697465"
          ),
          Map(
            "akka-remote" -> BundleDescriptor.Component.Endpoint("tcp", 0, None, Seq.empty),
            "es" -> BundleDescriptor.Component.Endpoint("http", 0, Some("elastic-search"), Seq.empty)
          )
        )
      )
    )
  }

  object WithoutTags {
    val hocon =
      """
        |compatibilityVersion="1"
        |components {
        |  eslite {
        |    description = eslite
        |    endpoints {
        |      akka-remote {
        |        bind-port = 0
        |        bind-protocol = tcp
        |        services = []
        |      }
        |      es {
        |        bind-port = 0
        |        bind-protocol = http
        |        service-name = elastic-search
        |        services = []
        |      }
        |    }
        |    file-system-type = universal
        |    start-command = [
        |      "eslite/bin/eslite",
        |      "-J-Xms134217728",
        |      "-J-Xmx134217728",
        |      "-Dhttp.address=$ES_BIND_IP",
        |      "-Dhttp.port=$ES_BIND_PORT",
        |      "-Dplay.crypto.secret=65736c697465"
        |    ]
        |  }
        |}
        |diskSpace = 200000000
        |memory = 402653184
        |name = eslite
        |nrOfCpus = 0.1
        |roles = [elasticsearch]
        |system = eslite
        |systemVersion = "1"
        |version = "1"
      """.stripMargin

    val bundleDescriptor = BundleDescriptor(
      version = "1",
      system = "eslite",
      systemVersion = "1",
      nrOfCpus = 0.1,
      memory = 402653184L,
      diskSpace = 200000000L,
      roles = Seq("elasticsearch"),
      bundleName = "eslite",
      compatibilityVersion = "1",
      tags = Seq.empty,
      annotations = None,
      components = Map(
        "eslite" -> new BundleDescriptor.Component(
          "eslite",
          BundleDescriptor.Component.FileSystemType.Universal,
          Seq(
            "eslite/bin/eslite",
            "-J-Xms134217728",
            "-J-Xmx134217728",
            "-Dhttp.address=$ES_BIND_IP",
            "-Dhttp.port=$ES_BIND_PORT",
            "-Dplay.crypto.secret=65736c697465"
          ),
          Map(
            "akka-remote" -> BundleDescriptor.Component.Endpoint("tcp", 0, None, Seq.empty),
            "es" -> BundleDescriptor.Component.Endpoint("http", 0, Some("elastic-search"), Seq.empty)
          )
        )
      )
    )
  }

  object WithTags {
    val hocon =
      """
        |compatibilityVersion="1"
        |components {
        |  eslite {
        |    description = eslite
        |    endpoints {
        |      akka-remote {
        |        bind-port = 0
        |        bind-protocol = tcp
        |        services = []
        |      }
        |      es {
        |        bind-port = 0
        |        bind-protocol = http
        |        service-name = elastic-search
        |        services = []
        |      }
        |    }
        |    file-system-type = universal
        |    start-command = [
        |      "eslite/bin/eslite",
        |      "-J-Xms134217728",
        |      "-J-Xmx134217728",
        |      "-Dhttp.address=$ES_BIND_IP",
        |      "-Dhttp.port=$ES_BIND_PORT",
        |      "-Dplay.crypto.secret=65736c697465"
        |    ]
        |  }
        |}
        |diskSpace = 200000000
        |memory = 402653184
        |name = eslite
        |nrOfCpus = 0.1
        |roles = [elasticsearch]
        |system = eslite
        |systemVersion = "1"
        |version = "1"
        |tags = ["1.5.1", "non-prod"]
      """.stripMargin

    val bundleDescriptor = BundleDescriptor(
      version = "1",
      system = "eslite",
      systemVersion = "1",
      nrOfCpus = 0.1,
      memory = 402653184L,
      diskSpace = 200000000L,
      roles = Seq("elasticsearch"),
      bundleName = "eslite",
      compatibilityVersion = "1",
      tags = Seq("1.5.1", "non-prod"),
      annotations = None,
      components = Map(
        "eslite" -> new BundleDescriptor.Component(
          "eslite",
          BundleDescriptor.Component.FileSystemType.Universal,
          Seq(
            "eslite/bin/eslite",
            "-J-Xms134217728",
            "-J-Xmx134217728",
            "-Dhttp.address=$ES_BIND_IP",
            "-Dhttp.port=$ES_BIND_PORT",
            "-Dplay.crypto.secret=65736c697465"
          ),
          Map(
            "akka-remote" -> BundleDescriptor.Component.Endpoint("tcp", 0, None, Seq.empty),
            "es" -> BundleDescriptor.Component.Endpoint("http", 0, Some("elastic-search"), Seq.empty)
          )
        )
      )
    )
  }

  object WithAnnotationsDashed {
    val hocon =
      """
        |compatibility-version="1"
        |components {
        |  eslite {
        |    description = eslite
        |    endpoints {
        |      akka-remote {
        |        bind-port = 0
        |        bind-protocol = tcp
        |        services = []
        |      }
        |      es {
        |        bind-port = 0
        |        bind-protocol = http
        |        service-name = elastic-search
        |        services = []
        |      }
        |    }
        |    file-system-type = universal
        |    start-command = [
        |      "eslite/bin/eslite",
        |      "-J-Xms134217728",
        |      "-J-Xmx134217728",
        |      "-Dhttp.address=$ES_BIND_IP",
        |      "-Dhttp.port=$ES_BIND_PORT",
        |      "-Dplay.crypto.secret=65736c697465"
        |    ]
        |  }
        |}
        |disk-space = 200000000
        |memory = 402653184
        |name = eslite
        |nr-of-cpus = 0.1
        |roles = [elasticsearch]
        |system = eslite
        |system-version = "1"
        |version = "1"
        |annotations = {
        |  hey = there
        |}
      """.stripMargin

    val bundleDescriptor = BundleDescriptor(
      version = "1",
      system = "eslite",
      systemVersion = "1",
      nrOfCpus = 0.1,
      memory = 402653184L,
      diskSpace = 200000000L,
      roles = Seq("elasticsearch"),
      bundleName = "eslite",
      compatibilityVersion = "1",
      tags = Seq.empty,
      annotations = Some(ConfigFactory.parseString("hey = there").root()),
      components = Map(
        "eslite" -> new BundleDescriptor.Component(
          "eslite",
          BundleDescriptor.Component.FileSystemType.Universal,
          Seq(
            "eslite/bin/eslite",
            "-J-Xms134217728",
            "-J-Xmx134217728",
            "-Dhttp.address=$ES_BIND_IP",
            "-Dhttp.port=$ES_BIND_PORT",
            "-Dplay.crypto.secret=65736c697465"
          ),
          Map(
            "akka-remote" -> BundleDescriptor.Component.Endpoint("tcp", 0, None, Seq.empty),
            "es" -> BundleDescriptor.Component.Endpoint("http", 0, Some("elastic-search"), Seq.empty)
          )
        )
      )

    )
  }

}

class BundleDescriptorSpec extends WordSpec with Matchers {
  import BundleDescriptorSpec._

  "fromConfig" should {
    Seq(
      WithoutTagsDashed.hocon -> WithoutTagsDashed.bundleDescriptor -> "without tags + dashed config keys",
      WithoutTags.hocon -> WithoutTags.bundleDescriptor -> "without tags",
      WithTags.hocon -> WithTags.bundleDescriptor -> "with tags",
      WithAnnotationsDashed.hocon -> WithAnnotationsDashed.bundleDescriptor -> "with annotations + dashed config keys"
    ).foreach {
        case ((hocon, expectedBundleDescriptor), scenario) =>
          s"build BundleDescriptor $scenario" in {
            val bundleDescriptorConfig = ConfigFactory.parseString(hocon).root()
            BundleDescriptor.fromConfig(bundleDescriptorConfig) shouldBe expectedBundleDescriptor
          }
      }

  }
}
