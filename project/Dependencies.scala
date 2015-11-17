import sbt._

object Version {
  val akka23      = "2.3.12"
  val akka23Http  = "1.0"
  val java8Compat = "0.7.0"
  val junit       = "4.12"
  val play23      = "2.3.9"
  val play24      = "2.4.2"
  val scala       = "2.11.7"
  val scalaTest   = "2.2.4"
}

object Library {
  val akka23Cluster     = "com.typesafe.akka"      %% "akka-cluster"                   % Version.akka23
  val akka23Http        = "com.typesafe.akka"      %% "akka-http-experimental"         % Version.akka23Http
  val akka23HttpTestkit = "com.typesafe.akka"      %% "akka-http-testkit-experimental" % Version.akka23Http
  val akka23Testkit     = "com.typesafe.akka"      %% "akka-testkit"                   % Version.akka23
  val java8Compat       = "org.scala-lang.modules" %% "scala-java8-compat"             % Version.java8Compat
  val junit             = "junit"                  %  "junit"                          % Version.junit
  val play23Test        = "com.typesafe.play"      %% "play-test"                      % Version.play23
  val play23Ws          = "com.typesafe.play"      %% "play-ws"                        % Version.play23
  val play24Test        = "com.typesafe.play"      %% "play-test"                      % Version.play24
  val play24Ws          = "com.typesafe.play"      %% "play-ws"                        % Version.play24
  val scalaTest         = "org.scalatest"          %% "scalatest"                      % Version.scalaTest
}

object Resolvers {
  val playTypesafeReleases = "play-typesafe-releases" at "http://repo.typesafe.com/typesafe/maven-releases"
}
