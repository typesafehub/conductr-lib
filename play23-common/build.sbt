name := "play23-conductr-lib-common"

libraryDependencies ++= List(
  Library.play23Ws
)

resolvers += Resolvers.typesafeReleases // For netty-http-pipeline

crossScalaVersions := crossScalaVersions.value.filterNot(_.startsWith("2.12"))
scalacOptions += "-target:jvm-1.6"