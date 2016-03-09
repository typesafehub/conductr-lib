import sbt._
import sbt.Resolver.bintrayRepo

object Version {
  val akka23             = "2.3.14"
  val akka23Http         = "2.0.3"
  val akka23Stream       = "2.0.3"
  val akka23Sse          = "1.5.0"
  val akka23ContribExtra = "2.0.2"
  val akka24             = "2.4.2"
  val akka24Sse          = "1.6.3"
  val akka24ContribExtra = "3.0.0"
  val java8Compat        = "0.7.0"
  val junit              = "4.12"
  val play23             = "2.3.10"
  val play24             = "2.4.6"
  val play25             = "2.5.0"
  val lagom10            = "0.1.0-SNAPSHOT"
  val scala              = "2.11.7"
  val scalaTest          = "2.2.6"
}

object Library {
  val akka23Cluster      = "com.typesafe.akka"      %% "akka-cluster"                   % Version.akka23
  val akka23Http         = "com.typesafe.akka"      %% "akka-http-experimental"         % Version.akka23Http
  val akka23HttpTestkit  = "com.typesafe.akka"      %% "akka-http-testkit-experimental" % Version.akka23Http
  val akka23Testkit      = "com.typesafe.akka"      %% "akka-testkit"                   % Version.akka23
  val akka23Sse          = "de.heikoseeberger"      %% "akka-sse"                       % Version.akka23Sse
  val akka23ContribExtra = "com.typesafe.akka"      %% "akka-contrib-extra"             % Version.akka23ContribExtra
  val akka24Cluster      = "com.typesafe.akka"      %% "akka-cluster"                   % Version.akka24
  val akka24Http         = "com.typesafe.akka"      %% "akka-http-experimental"         % Version.akka24
  val akka24HttpTestkit  = "com.typesafe.akka"      %% "akka-http-testkit"              % Version.akka24
  val akka24Testkit      = "com.typesafe.akka"      %% "akka-testkit"                   % Version.akka24
  val akka24Sse          = "de.heikoseeberger"      %% "akka-sse"                       % Version.akka24Sse
  val akka24ContribExtra = "com.typesafe.akka"      %% "akka-contrib-extra"             % Version.akka24ContribExtra
  val java8Compat        = "org.scala-lang.modules" % "scala-java8-compat_2.11"         % Version.java8Compat
  val junit              = "junit"                  %  "junit"                          % Version.junit
  val play23Test         = "com.typesafe.play"      %% "play-test"                      % Version.play23
  val play23Ws           = "com.typesafe.play"      %% "play-ws"                        % Version.play23
  val play23Json         = "com.typesafe.play"      %% "play-json"                      % Version.play23
  val play24Test         = "com.typesafe.play"      %% "play-test"                      % Version.play24
  val play24Ws           = "com.typesafe.play"      %% "play-ws"                        % Version.play24
  val play24Json         = "com.typesafe.play"      %% "play-json"                      % Version.play24
  val play25Test         = "com.typesafe.play"      %% "play-test"                      % Version.play25
  val play25Json         = "com.typesafe.play"      %% "play-json"                      % Version.play25
  val play25Ws           = "com.typesafe.play"      %% "play-ws"                        % Version.play25
  val lagom10Api         = "com.newco.lagom"        %% "lagom-javadsl-api"              % Version.lagom10
  val scalaTest          = "org.scalatest"          %% "scalatest"                      % Version.scalaTest
}

object Resolvers {
  val hseeberger                 = bintrayRepo("hseeberger", "maven")
  val typesafeBintrayReleases    = bintrayRepo("typesafe", "maven-releases")
  val playTypesafeReleases       = "play-typesafe-releases" at "http://repo.typesafe.com/typesafe/maven-releases"
}
