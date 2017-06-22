package com.typesafe.conductr.clientlib.akka

import java.net.{ URL, URI }
import java.util.UUID
import com.typesafe.conductr.clientlib.scala.models.HttpRequestMapping.{ Path, PathBeg, PathRegex }
import com.typesafe.conductr.clientlib.scala.models._
import com.typesafe.conductr.lib.scala.ConductrTypeOps
import java.util.Date
import scala.collection.immutable.SortedSet

object TestData {

  import ConductrTypeOps._

  private val Host = new URL(s"http://127.0.0.1:34567")

  val RequestId = UUID.randomUUID()

  val BundleFileName = "typesafe-conductr-tester-v0-5dd6695ed93ea6f10d856a97e2e90b56eb28bdc7d98555be944066b83f536a55.zip"
  val BundleUri = TestData.getClass.getClassLoader.getResource(BundleFileName).toURI

  val BundleSystem = "some-system"
  val BundleSystemVersion = "1.0.0"
  val BundleCompatibilityVersion = "1"
  val BundleName = "typesafe-conductr-tester"
  val ConfigurationName = "configuration"

  val Digest = "023f9da2243a0751c2e231b452aa3ed32fbc35351c543fbd536eea7ec457cfe2"

  val BundleFrontend = Bundle(
    bundleId = "5dd6695ed93ea6f10d856a97e2e90b56eb28bdc7d98555be944066b83f536a55",
    bundleDigest = Digest,
    configurationDigest = None,
    attributes = BundleAttributes(
      system = BundleSystem,
      nrOfCpus = 1.0,
      memory = 1024000,
      diskSpace = 64000,
      roles = SortedSet("frontend"),
      bundleName = BundleName,
      systemVersion = BundleSystemVersion,
      compatibilityVersion = BundleCompatibilityVersion,
      tags = Seq.empty
    ),
    bundleConfig = Some(BundleConfig(Map(
      "ep1" -> BundleConfigEndpoint("http", Some("webster"), Set(new URI("http://:8000/webster")), Seq.empty)
    ))),
    bundleScale = Some(BundleScale(1, None)),
    bundleExecutions = Iterable(
      BundleExecution(
        host = Host.getHost,
        endpoints = Map(
          "ep1" -> BundleExecutionEndpoint(9011, 9011)
        ),
        isStarted = true
      )
    ),
    bundleInstallations = Iterable(
      BundleInstallation(
        uniqueAddress = UniqueAddress(new URI(s"akka.tcp://conductr@${Host.getHost}:${Host.getPort}"), 123),
        bundleFile = BundleUri,
        configurationFile = None
      )
    ),
    hasError = false
  )

  val BundleFrontendAsJson =
    s"""
       |{
       |  "bundleId": "${BundleFrontend.bundleId}",
       |  "bundleDigest": "$Digest",
       |  "attributes": {
       |    "system": "$BundleSystem",
       |    "nrOfCpus": 1,
       |    "memory": 1024000,
       |    "diskSpace": 64000,
       |    "roles": [
       |      "frontend"
       |    ],
       |    "bundleName": "$BundleName",
       |    "systemVersion": "$BundleSystemVersion",
       |    "compatibilityVersion": "$BundleCompatibilityVersion"
       |  },
       |  "bundleConfig": {
       |    "endpoints": {
       |      "ep1": {
       |        "bindProtocol": "http",
       |        "serviceName": "webster",
       |        "services": [
       |          "http://:8000/webster"
       |        ]
       |      }
       |    }
       |  },
       |  "bundleScale": {
       |    "scale": 1
       |  },
       |  "bundleExecutions": [
       |    {
       |      "host": "${Host.getHost}",
       |      "endpoints": {
       |        "ep1": {
       |          "bindPort": 9011,
       |          "hostPort": 9011
       |        }
       |      },
       |      "isStarted": true
       |    }
       |  ],
       |  "bundleInstallations": [
       |    {
       |      "uniqueAddress": {
       |        "address": "akka.tcp://conductr@${Host.getHost}:${Host.getPort}",
       |        "uid": 123
       |      },
       |      "bundleFile": "$BundleUri"
       |    }
       |  ],
       |  "hasError": false
       |}
     """.stripMargin

  val BundleFrontendNoExecutionAsJson =
    s"""
       |{
       |  "bundleId": "${BundleFrontend.bundleId}",
       |  "bundleDigest": "$Digest",
       |  "attributes": {
       |    "system": "$BundleSystem",
       |    "nrOfCpus": 1,
       |    "memory": 1024000,
       |    "diskSpace": 64000,
       |    "roles": [
       |      "frontend"
       |    ],
       |    "bundleName": "$BundleName",
       |    "systemVersion": "$BundleSystemVersion",
       |    "compatibilityVersion": "$BundleCompatibilityVersion"
       |  },
       |  "bundleConfig": {
       |    "endpoints": {
       |      "ep1": {
       |        "bindProtocol": "http",
       |        "serviceName": "webster",
       |        "services": [
       |          "http://:8000/webster"
       |        ]
       |      }
       |    }
       |  },
       |  "bundleScale": {
       |    "scale": 1
       |  },
       |  "bundleExecutions": [
       |    {
       |      "host": "${Host.getHost}",
       |      "endpoints": {
       |        "ep1": {
       |          "bindPort": 9011,
       |          "hostPort": 9011
       |        }
       |      },
       |      "isStarted": false
       |    }
       |  ],
       |  "bundleInstallations": [
       |    {
       |      "uniqueAddress": {
       |        "address": "akka.tcp://conductr@${Host.getHost}:${Host.getPort}",
       |        "uid": 123
       |      },
       |      "bundleFile": "$BundleUri"
       |    }
       |  ],
       |  "hasError": false
       |}
     """.stripMargin

  val BundleBackend = Bundle(
    bundleId = "5dd6695ed93ea6f10d856a97e2e90b56eb28bdc7d98555be944066b83f536a56",
    bundleDigest = Digest,
    configurationDigest = None,
    attributes = BundleAttributes(
      system = BundleSystem,
      nrOfCpus = 2.0,
      memory = 1024000,
      diskSpace = 64000,
      roles = SortedSet("backend"),
      bundleName = "backend",
      systemVersion = BundleSystemVersion,
      compatibilityVersion = BundleCompatibilityVersion,
      tags = Seq.empty
    ),
    bundleConfig = Some(BundleConfig(Map(
      "ep1" -> BundleConfigEndpoint("http", None, Set(new URI("http://:5555")), Seq.empty)
    ))),
    bundleScale = None,
    bundleExecutions = Iterable.empty,
    bundleInstallations = Iterable(
      BundleInstallation(
        uniqueAddress = UniqueAddress(new URI(s"akka.tcp://conductr@${Host.getHost}:${Host.getPort}"), 456),
        bundleFile = BundleUri,
        configurationFile = None
      )
    ),
    hasError = false
  )

  val BundleBackendAsJson =
    s"""
       |{
       |  "bundleId": "${BundleBackend.bundleId}",
       |  "bundleDigest": "$Digest",
       |  "attributes": {
       |    "system": "$BundleSystem",
       |    "nrOfCpus": 2,
       |    "memory": 1024000,
       |    "diskSpace": 64000,
       |    "roles": [
       |      "backend"
       |    ],
       |    "bundleName": "backend",
       |    "systemVersion": "$BundleSystemVersion",
       |    "compatibilityVersion": "$BundleCompatibilityVersion"
       |  },
       |  "bundleConfig": {
       |    "endpoints": {
       |      "ep1": {
       |        "bindProtocol": "http",
       |        "services": [
       |          "http://:5555"
       |        ]
       |      }
       |    }
       |  },
       |  "bundleExecutions": [],
       |  "bundleInstallations": [
       |    {
       |      "uniqueAddress": {
       |        "address": "akka.tcp://conductr@${Host.getHost}:${Host.getPort}",
       |        "uid": 456
       |      },
       |      "bundleFile": "$BundleUri"
       |    }
       |  ],
       |  "hasError": false
       |}
     """.stripMargin

  val BundleWithServicesAndRequestAcl = Bundle(
    bundleId = "98409f27bacd3e5fb334961999497a02025038b0985c384bb5d86417b246c773",
    bundleDigest = Digest,
    configurationDigest = None,
    attributes = BundleAttributes(
      system = BundleSystem,
      nrOfCpus = 2.0,
      memory = 1024000,
      diskSpace = 64000,
      roles = SortedSet("hub"),
      bundleName = "hub",
      systemVersion = BundleSystemVersion,
      compatibilityVersion = BundleCompatibilityVersion,
      tags = Seq.empty
    ),
    bundleConfig = Some(BundleConfig(Map(
      "old-hub" -> BundleConfigEndpoint("http", Some("old-hub"), Set(new URI("http://:5555/hub1")), Seq.empty),
      "hub" -> BundleConfigEndpoint("http", Some("hub"), Set.empty, Seq(
        RequestAcl(Seq(
          HttpFamilyRequestMappings(Seq(
            Path("/path-1"),
            Path("/path-2", method = Some("GET"), rewrite = Some("/other-path-2")),
            PathBeg("/path-beg-1"),
            PathBeg("/path-beg-2", method = Some("GET"), rewrite = Some("/other-path-beg-2")),
            PathRegex("/path-regex-1"),
            PathRegex("/path-regex-2", method = Some("GET"), rewrite = Some("/other-path-regex-2"))
          ))
        ))
      )),
      "tunnel" -> BundleConfigEndpoint("tcp", None, Set.empty, Seq(
        RequestAcl(Seq(
          TcpFamilyRequestMappings(Seq(
            TcpRequestMapping(7001)
          ))
        ))
      )),
      "broadcast" -> BundleConfigEndpoint("udp", None, Set.empty, Seq(
        RequestAcl(Seq(
          UdpFamilyRequestMappings(Seq(
            UdpRequestMapping(5001)
          ))
        ))
      ))
    ))),
    bundleScale = None,
    bundleExecutions = Iterable.empty,
    bundleInstallations = Iterable(
      BundleInstallation(
        uniqueAddress = UniqueAddress(new URI(s"akka.tcp://conductr@${Host.getHost}:${Host.getPort}"), 456),
        bundleFile = BundleUri,
        configurationFile = None
      )
    ),
    hasError = false
  )

  val BundleWithServicesAndRequestAclJson =
    s"""
      |{
      |    "bundleId": "98409f27bacd3e5fb334961999497a02025038b0985c384bb5d86417b246c773",
      |    "bundleDigest": "$Digest",
      |    "attributes": {
      |      "system": "$BundleSystem",
      |      "nrOfCpus": 2.0,
      |      "memory": 1024000,
      |      "diskSpace": 64000,
      |      "roles": [
      |        "hub"
      |      ],
      |      "bundleName": "hub",
      |      "systemVersion": "$BundleSystemVersion",
      |      "compatibilityVersion": "$BundleCompatibilityVersion"
      |    },
      |    "bundleConfig": {
      |      "endpoints": {
      |        "old-hub": {
      |          "bindProtocol": "http",
      |          "serviceName": "old-hub",
      |          "services": [
      |            "http://:5555/hub1"
      |          ]
      |        },
      |        "hub": {
      |          "bindProtocol": "http",
      |          "serviceName": "hub",
      |          "acls": [
      |            {
      |              "http": {
      |                "requests": [
      |                  { "path": "/path-1" },
      |                  { "path": "/path-2", "method": "GET", "rewrite": "/other-path-2" },
      |                  { "pathBeg": "/path-beg-1" },
      |                  { "pathBeg": "/path-beg-2", "method": "GET", "rewrite": "/other-path-beg-2" },
      |                  { "pathRegex": "/path-regex-1" },
      |                  { "pathRegex": "/path-regex-2", "method": "GET", "rewrite": "/other-path-regex-2" }
      |                ]
      |              }
      |            }
      |          ]
      |        },
      |        "tunnel": {
      |          "bindProtocol": "tcp",
      |          "acls": [
      |            {
      |              "tcp": {
      |                "requests": [7001]
      |              }
      |            }
      |          ]
      |        },
      |        "broadcast": {
      |          "bindProtocol": "udp",
      |          "acls": [
      |            {
      |              "udp": {
      |                "requests": [5001]
      |              }
      |            }
      |          ]
      |        }
      |      }
      |    },
      |    "bundleExecutions": [],
      |    "bundleInstallations": [
      |      {
      |        "uniqueAddress": {
      |          "address": "akka.tcp://conductr@${Host.getHost}:${Host.getPort}",
      |          "uid": 456
      |        },
      |        "bundleFile": "$BundleUri"
      |      }
      |    ],
      |    "hasError": false
      |  }
    """.stripMargin

  val BundleEvents = Seq(
    BundleEvent(
      timestamp = new Date(1452614899549l),
      event = "conductr.loadExecutor.bundleWritten",
      description = "Bundle written: requestId=53fd9495-79cb-4098-b93a-ac66b8eb7b73, bundleName=conductr-elasticsearch, bundleId=ec1f9e50809bada6e1188fec7fe20d1f"
    ),
    BundleEvent(
      timestamp = new Date(1452614901104l),
      event = "conductr.scaleScheduler.scaleBundleRequested",
      description = "Scale bundle requested: requestId=258ee94e-2b45-4e33-aef3-8f1ba054f39d, bundleId=ec1f9e50809bada6e1188fec7fe20d1f, scale=1"
    )
  )

  val BundleEventsAsJson =
    s"""
       |[
       |  {
       |    "timestamp": "2016-01-12T16:08:19.549Z",
       |    "event": "conductr.loadExecutor.bundleWritten",
       |    "description": "Bundle written: requestId=53fd9495-79cb-4098-b93a-ac66b8eb7b73, bundleName=conductr-elasticsearch, bundleId=ec1f9e50809bada6e1188fec7fe20d1f"
       |  },
       |  {
       |    "timestamp": "2016-01-12T16:08:21.104Z",
       |    "event": "conductr.scaleScheduler.scaleBundleRequested",
       |    "description": "Scale bundle requested: requestId=258ee94e-2b45-4e33-aef3-8f1ba054f39d, bundleId=ec1f9e50809bada6e1188fec7fe20d1f, scale=1"
       |  }
       |]
     """.stripMargin

  val BundleLogs = Seq(
    BundleLog(
      timestamp = new Date(1452614899549l),
      host = "78a1db1ae29a",
      message = "[2016-01-12 16:08:58,651][INFO ][cluster.metadata] [Myron MacLain] [conductr] update_mapping [rfc5424] (dynamic)"
    ),
    BundleLog(
      timestamp = new Date(1452614901104l),
      host = "78a1db1ae29a",
      message = "[2016-01-12 16:08:30,268][INFO ][cluster.metadata] [Myron MacLain] [conductr] update_mapping [rfc5424] (dynamic)"
    )
  )

  val BundleLogsAsJson =
    s"""
       |[
       |  {
       |    "timestamp": "2016-01-12T16:08:19.549Z",
       |    "host": "78a1db1ae29a",
       |    "message": "[2016-01-12 16:08:58,651][INFO ][cluster.metadata] [Myron MacLain] [conductr] update_mapping [rfc5424] (dynamic)"
       |  },
       |  {
       |    "timestamp": "2016-01-12T16:08:21.104Z",
       |    "host": "78a1db1ae29a",
       |    "message": "[2016-01-12 16:08:30,268][INFO ][cluster.metadata] [Myron MacLain] [conductr] update_mapping [rfc5424] (dynamic)"
       |  }
       |]
     """.stripMargin

  val MemberUpAddress = new URI("akka.tcp://conductr@10.0.1.208:9004")
  val MemberUpUid = -1207279467
  val MemberUpUniqueAddress = UniqueAddress(MemberUpAddress, MemberUpUid)
  val MemberUp = Member(
    node = MemberUpUniqueAddress,
    status = "Up",
    roles = Set("frontend")
  )

  val MemberDownAddress = new URI("akka.tcp://conductr@10.0.1.209:9004")
  val MemberDownUid = -1207279468
  val MemberDownUniqueAddress = UniqueAddress(MemberDownAddress, MemberDownUid)
  val MemberDown = Member(
    node = MemberDownUniqueAddress,
    status = "Down",
    roles = Set("backend")
  )

  val MemberInfo =
    MemberInfoSuccess(
      member = MemberUp,
      isUnreachableFrom = Seq(MemberDownUniqueAddress),
      detectedUnreachable = Seq(MemberDownAddress)
    )

  val MemberInfoAsJson =
    s"""
       |{
       |  "node": {
       |    "address": "$MemberUpAddress",
       |    "uid": $MemberUpUid
       |  },
       |  "roles": [
       |    "frontend"
       |  ],
       |  "status": "Up",
       |  "isUnreachableFrom": [
       |    {
       |      "address": "$MemberDownAddress",
       |      "uid": $MemberDownUid
       |    }
       |  ],
       |  "detectedUnreachable": [
       |    "$MemberDownAddress"
       |  ]
       |}
     """.stripMargin

  val MembersInfo = MembersInfoSuccess(
    selfNode = MemberUpUniqueAddress,
    members = Seq(MemberUp, MemberDown),
    unreachable = Seq(UnreachableMember(
      node = MemberDownUniqueAddress,
      observedBy = Seq(MemberUpUniqueAddress)
    ))
  )

  val MembersInfoAsJson =
    s"""
       |{
       |  "selfNode": {
       |    "address": "$MemberUpAddress",
       |    "uid": $MemberUpUid
       |  },
       |  "members": [
       |    {
       |      "node": {
       |        "address": "$MemberUpAddress",
       |        "uid": $MemberUpUid
       |      },
       |      "status": "Up",
       |      "roles": [
       |        "frontend"
       |      ]
       |    },
       |    {
       |      "node": {
       |        "address": "$MemberDownAddress",
       |        "uid": $MemberDownUid
       |      },
       |      "status": "Down",
       |      "roles": [
       |        "backend"
       |      ]
       |    }
       |  ],
       |  "unreachable": [
       |    {
       |      "node": {
       |        "address": "$MemberDownAddress",
       |        "uid": $MemberDownUid
       |      },
       |      "observedBy": [
       |        {
       |          "address": "$MemberUpAddress",
       |          "uid": $MemberUpUid
       |        }
       |      ]
       |    }
       |  ]
       |}
     """.stripMargin

  val BundleWithTags = Bundle(
    bundleId = "98409f27bacd3e5fb334961999497a02025038b0985c384bb5d86417b246c773",
    bundleDigest = Digest,
    configurationDigest = None,
    attributes = BundleAttributes(
      system = BundleSystem,
      nrOfCpus = 2.0,
      memory = 1024000,
      diskSpace = 64000,
      roles = SortedSet("hub"),
      bundleName = "hub",
      systemVersion = BundleSystemVersion,
      compatibilityVersion = BundleCompatibilityVersion,
      tags = Seq("0.1.0", "apples")
    ),
    bundleConfig = Some(BundleConfig(Map(
      "hub" -> BundleConfigEndpoint("http", Some("hub"), Set.empty, Seq(
        RequestAcl(Seq(
          HttpFamilyRequestMappings(Seq(
            Path("/path-1"),
            Path("/path-2", method = Some("GET"), rewrite = Some("/other-path-2")),
            PathBeg("/path-beg-1"),
            PathBeg("/path-beg-2", method = Some("GET"), rewrite = Some("/other-path-beg-2")),
            PathRegex("/path-regex-1"),
            PathRegex("/path-regex-2", method = Some("GET"), rewrite = Some("/other-path-regex-2"))
          ))
        ))
      )),
      "tunnel" -> BundleConfigEndpoint("tcp", None, Set.empty, Seq(
        RequestAcl(Seq(
          TcpFamilyRequestMappings(Seq(
            TcpRequestMapping(7001)
          ))
        ))
      )),
      "broadcast" -> BundleConfigEndpoint("udp", None, Set.empty, Seq(
        RequestAcl(Seq(
          UdpFamilyRequestMappings(Seq(
            UdpRequestMapping(5001)
          ))
        ))
      ))
    ))),
    bundleScale = None,
    bundleExecutions = Iterable.empty,
    bundleInstallations = Iterable(
      BundleInstallation(
        uniqueAddress = UniqueAddress(new URI(s"akka.tcp://conductr@${Host.getHost}:${Host.getPort}"), 456),
        bundleFile = BundleUri,
        configurationFile = None
      )
    ),
    hasError = false
  )

  val BundleWithTagsJson =
    s"""
       |{
       |    "bundleId": "98409f27bacd3e5fb334961999497a02025038b0985c384bb5d86417b246c773",
       |    "bundleDigest": "$Digest",
       |    "attributes": {
       |      "system": "$BundleSystem",
       |      "nrOfCpus": 2.0,
       |      "memory": 1024000,
       |      "diskSpace": 64000,
       |      "roles": [
       |        "hub"
       |      ],
       |      "bundleName": "hub",
       |      "systemVersion": "$BundleSystemVersion",
       |      "compatibilityVersion": "$BundleCompatibilityVersion",
       |      "tags": [
       |        "0.1.0",
       |        "apples"
       |      ]
       |    },
       |    "bundleConfig": {
       |      "endpoints": {
       |        "hub": {
       |          "bindProtocol": "http",
       |          "serviceName": "hub",
       |          "acls": [
       |            {
       |              "http": {
       |                "requests": [
       |                  { "path": "/path-1" },
       |                  { "path": "/path-2", "method": "GET", "rewrite": "/other-path-2" },
       |                  { "pathBeg": "/path-beg-1" },
       |                  { "pathBeg": "/path-beg-2", "method": "GET", "rewrite": "/other-path-beg-2" },
       |                  { "pathRegex": "/path-regex-1" },
       |                  { "pathRegex": "/path-regex-2", "method": "GET", "rewrite": "/other-path-regex-2" }
       |                ]
       |              }
       |            }
       |          ]
       |        },
       |        "tunnel": {
       |          "bindProtocol": "tcp",
       |          "acls": [
       |            {
       |              "tcp": {
       |                "requests": [7001]
       |              }
       |            }
       |          ]
       |        },
       |        "broadcast": {
       |          "bindProtocol": "udp",
       |          "acls": [
       |            {
       |              "udp": {
       |                "requests": [5001]
       |              }
       |            }
       |          ]
       |        }
       |      }
       |    },
       |    "bundleExecutions": [],
       |    "bundleInstallations": [
       |      {
       |        "uniqueAddress": {
       |          "address": "akka.tcp://conductr@${Host.getHost}:${Host.getPort}",
       |          "uid": 456
       |        },
       |        "bundleFile": "$BundleUri"
       |      }
       |    ],
       |    "hasError": false
       |  }
    """.stripMargin

}
