import sbt._
import sbt.Resolver.bintrayRepo

object Version {
  val akka24             = "2.4.19"
  val akka24Sse          = "1.11.0"
  val akka24Http         = "10.0.8"
  val akka25             = "2.5.3"
  val akka25Http         = "10.0.8"
  val akka24ContribExtra = "3.3.2"
  val akka25ContribExtra = "4.0.0"
  val java8Compat        = "0.8.0"
  val junit              = "4.12"
  val play24             = "2.4.11"
  val play25             = "2.5.15"
  val play26             = "2.6.0"
  val lagom13            = "1.3.5"
  val lagom14            = "1.4.0-M3"
  val reactiveStreams    = "1.0.0"
  val typesafeConfig     = "1.3.0"
  val scala              = "2.11.8"
  val scalaTest          = "3.0.1"
}

object Library {
  val akka24Cluster         = "com.typesafe.akka"      %% "akka-cluster"                   % Version.akka24
  val akka24Http            = "com.typesafe.akka"      %% "akka-http"                      % Version.akka24Http
  val akka24Testkit         = "com.typesafe.akka"      %% "akka-testkit"                   % Version.akka24
  val akka24HttpTestkit     = "com.typesafe.akka"      %% "akka-http-testkit"              % Version.akka24Http
  val akka24Sse             = "de.heikoseeberger"      %% "akka-sse"                       % Version.akka24Sse
  val akka25Cluster         = "com.typesafe.akka"      %% "akka-cluster"                   % Version.akka25
  val akka25DistributedData = "com.typesafe.akka"      %% "akka-distributed-data"          % Version.akka25
  val akka25Stream          = "com.typesafe.akka"      %% "akka-stream"                     % Version.akka25
  val akka25Http            = "com.typesafe.akka"      %% "akka-http"                      % Version.akka25Http
  val akka25Testkit         = "com.typesafe.akka"      %% "akka-testkit"                   % Version.akka25
  val akka25StreamTestkit   = "com.typesafe.akka"      %% "akka-stream-testkit"            % Version.akka25
  val akka25HttpTestkit     = "com.typesafe.akka"      %% "akka-http-testkit"              % Version.akka25Http
  val akka24ContribExtra    = "com.typesafe.akka"      %% "akka-contrib-extra"             % Version.akka24ContribExtra
  val akka25ContribExtra    = "com.typesafe.akka"      %% "akka-contrib-extra"             % Version.akka25ContribExtra
  val lagom13ClientJavadsl  = "com.lightbend.lagom"    %% "lagom-javadsl-client"           % Version.lagom13
  val lagom13ClientScaladsl = "com.lightbend.lagom"    %% "lagom-scaladsl-client"          % Version.lagom13
  val lagom13ServerScaladsl = "com.lightbend.lagom"    %% "lagom-scaladsl-server"          % Version.lagom13
  val lagom14ClientJavadsl  = "com.lightbend.lagom"    %% "lagom-javadsl-client"           % Version.lagom14
  val lagom14ClientScaladsl = "com.lightbend.lagom"    %% "lagom-scaladsl-client"          % Version.lagom14
  val lagom14ServerScaladsl = "com.lightbend.lagom"    %% "lagom-scaladsl-server"          % Version.lagom14
  val java8Compat           = "org.scala-lang.modules" %% "scala-java8-compat"             % Version.java8Compat
  val reactiveStreams       = "org.reactivestreams"    % "reactive-streams"                % Version.reactiveStreams
  val junit                 = "junit"                  %  "junit"                          % Version.junit
  val play24Test            = "com.typesafe.play"      %% "play-test"                      % Version.play24
  val play24Ws              = "com.typesafe.play"      %% "play-ws"                        % Version.play24
  val play24Json            = "com.typesafe.play"      %% "play-json"                      % Version.play24
  val play25Test            = "com.typesafe.play"      %% "play-test"                      % Version.play25
  val play25Json            = "com.typesafe.play"      %% "play-json"                      % Version.play25
  val play25Ws              = "com.typesafe.play"      %% "play-ws"                        % Version.play25
  val play26Guice           = "com.typesafe.play"      %% "play-guice"                     % Version.play26
  val play26Test            = "com.typesafe.play"      %% "play-test"                      % Version.play26
  val play26Json            = "com.typesafe.play"      %% "play-json"                      % Version.play26
  val playWsStandalone      = "com.typesafe.play"      %% "play-ahc-ws"                    % Version.play26
  val typesafeConfig        = "com.typesafe"           %  "config"                         % Version.typesafeConfig
  val scalaTest             = "org.scalatest"          %% "scalatest"                      % Version.scalaTest
}

object Resolvers {
  val hseeberger       = bintrayRepo("hseeberger", "maven")
  val typesafeReleases = Resolver.typesafeRepo("releases")
}
