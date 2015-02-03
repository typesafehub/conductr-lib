/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

import sbt._

object Version {
  val scala     = "2.11.5"
  val scalaTest = "2.2.3"
}

object Library {
  val scalaTest       = "org.scalatest"     %% "scalatest"                      % Version.scalaTest
}

object Resolver {
}
