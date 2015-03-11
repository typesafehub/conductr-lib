/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

import sbt.Resolver._
import sbt._

object Version {
  val scala     = "2.11.6"
  val scalaTest = "2.2.3"
  val akka      = "2.3.9"
  val akkaHttp  = "1.0-M4"
  val play      = "2.3.8"
  val junit     = "4.12"
}

object Library {
  val akkaCluster     = "com.typesafe.akka" %% "akka-cluster"                   % Version.akka
  val akkaHttp        = "com.typesafe.akka" %% "akka-http-experimental"         % Version.akkaHttp
  val akkaTestkit     = "com.typesafe.akka" %% "akka-testkit"                   % Version.akka
  val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit-experimental" % Version.akkaHttp
  val playTest        = "com.typesafe.play" %% "play-test"                      % Version.play
  val playWs          = "com.typesafe.play" %% "play-ws"                        % Version.play
  val junit           = "junit"             %  "junit"                          % Version.junit
  val scalaTest       = "org.scalatest"     %% "scalatest"                      % Version.scalaTest
}

object Resolvers {
  val typesafeReleases = "typesafe-releases" at "http://repo.typesafe.com/typesafe/maven-releases"
}