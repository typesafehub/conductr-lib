name := "play23-conductr-lib-common"

libraryDependencies ++= List(
  Library.play23Ws
)

resolvers += Resolvers.typesafeReleases // For netty-http-pipeline