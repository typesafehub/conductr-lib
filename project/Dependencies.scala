/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

import sbt._

object Version {
  val scala     = "2.11.5"
  val scalaTest = "2.2.3"
  val akka      = "2.3.9"
  val akkaHttp  = "1.0-M2"
}

object Library {
  val akkaCluster     = "com.typesafe.akka" %% "akka-cluster"                   % Version.akka
  val akkaTestkit     = "com.typesafe.akka" %% "akka-testkit"                   % Version.akka
  val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit-experimental" % Version.akkaHttp
  val scalaTest       = "org.scalatest"     %% "scalatest"                      % Version.scalaTest
}
