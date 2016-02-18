import sbt._
import sbt.Resolver.bintrayRepo

object Version {
  val akka23           = "2.3.14"
  val akka23Http       = "2.0.3"
  val akka23Stream     = "2.0.3"
  val akkaSse23        = "1.5.0"
  val akkaContribExtra = "2.0.2"
  val akka24           = "2.4.2"
  val akkaSse24        = "1.6.3"
  val java8Compat      = "0.7.0"
  val junit            = "4.12"
  val play23           = "2.3.10"
  val play24           = "2.4.4"
  val play25           = "2.5.0-RC1"
  val scala            = "2.11.7"
  val scalaTest        = "2.2.6"
}

object Library {
  val akka23Cluster     = "com.typesafe.akka"      %% "akka-cluster"                   % Version.akka23
  val akka23Http        = "com.typesafe.akka"      %% "akka-http-experimental"         % Version.akka23Http
  val akka23HttpTestkit = "com.typesafe.akka"      %% "akka-http-testkit-experimental" % Version.akka23Http
  val akkaStream        = "com.typesafe.akka"      %% "akka-stream-experimental"       % Version.akka23Stream
  val akka23Testkit     = "com.typesafe.akka"      %% "akka-testkit"                   % Version.akka23
  val akkaSse23         = "de.heikoseeberger"      %% "akka-sse"                       % Version.akkaSse23
  val akkaContribExtra  = "com.typesafe.akka"      %% "akka-contrib-extra"             % Version.akkaContribExtra
  val akka24Cluster     = "com.typesafe.akka"      %% "akka-cluster"                   % Version.akka24
  val akka24Http        = "com.typesafe.akka"      %% "akka-http-experimental"         % Version.akka24
  val akka24HttpTestkit = "com.typesafe.akka"      %% "akka-http-testkit-experimental" % Version.akka24
  val akka24Stream      = "com.typesafe.akka"      %% "akka-stream-experimental"       % Version.akka24
  val akka24Testkit     = "com.typesafe.akka"      %% "akka-testkit"                   % Version.akka24
  val akkaSse24         = "de.heikoseeberger"      %% "akka-sse"                       % Version.akkaSse24
  val java8Compat       = "org.scala-lang.modules" % "scala-java8-compat_2.11"         % Version.java8Compat
  val junit             = "junit"                  %  "junit"                          % Version.junit
  val play23Test        = "com.typesafe.play"      %% "play-test"                      % Version.play23
  val play23Ws          = "com.typesafe.play"      %% "play-ws"                        % Version.play23
  val play23Json        = "com.typesafe.play"      %% "play-json"                      % Version.play23
  val play24Test        = "com.typesafe.play"      %% "play-test"                      % Version.play24
  val play24Ws          = "com.typesafe.play"      %% "play-ws"                        % Version.play24
  val play24Json        = "com.typesafe.play"      %% "play-json"                      % Version.play24
  val play25Test        = "com.typesafe.play"      %% "play-test"                      % Version.play25
  val play25Json        = "com.typesafe.play"      %% "play-json"                      % Version.play25
  val play25Ws          = "com.typesafe.play"      %% "play-ws"                        % Version.play25
  val scalaTest         = "org.scalatest"          %% "scalatest"                      % Version.scalaTest
}

object Resolvers {
  val hseeberger                 = bintrayRepo("hseeberger", "maven")
  val typesafeBintrayReleases    = bintrayRepo("typesafe", "maven-releases")
  val playTypesafeReleases       = "play-typesafe-releases" at "http://repo.typesafe.com/typesafe/maven-releases"
  // FIXME remove this when we don't use Play 2.5 snapshot
  val sonatypeOssSnapshot        = "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
}
