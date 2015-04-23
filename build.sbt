/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

lazy val root = project
  .in(file("."))
  .aggregate(
    conductRBundleLib,
    scalaConductRBundleLib,
    akka23ConductRBundleLib,
    play23ConductRBundleLib,
    play24ConductRBundleLib)

lazy val conductRBundleLib = project
  .in(file("conductr-bundle-lib"))
  .dependsOn(testLib % "test->compile")

lazy val scalaConductRBundleLib = project
  .in(file("scala-conductr-bundle-lib"))
  .dependsOn(conductRBundleLib)
  .dependsOn(testLib % "test->compile")

lazy val akka23ConductRBundleLib = project
  .in(file("akka23-conductr-bundle-lib"))
  .dependsOn(scalaConductRBundleLib)
  .dependsOn(testLib % "test->compile")

lazy val play23ConductRBundleLib = project
  .in(file("play23-conductr-bundle-lib"))
  .dependsOn(scalaConductRBundleLib)
  .dependsOn(testLib % "test->compile")

lazy val play24ConductRBundleLib = project
  .in(file("play24-conductr-bundle-lib"))
  .dependsOn(scalaConductRBundleLib)
  .dependsOn(testLib % "test->compile")

lazy val testLib = project
  .in(file("test-lib"))
  
name := "root"

publishArtifact := false
