lazy val root = project
  .in(file("."))
  .aggregate(
    common,
    conductRBundleLib,
    scalaConductRBundleLib,
    akka23ConductRBundleLib,
    play23ConductRBundleLib,
    play24ConductRBundleLib)


// Base
lazy val common = project
  .in(file("common"))
  .dependsOn(testLib % "test->compile")

lazy val conductRBundleLib = project
  .in(file("conductr-bundle-lib"))
  .dependsOn(common)
  .dependsOn(testLib % "test->compile")


// Scala
lazy val scalaCommon = project
  .in(file("scala-common"))
  .dependsOn(common)

lazy val scalaConductRBundleLib = project
  .in(file("scala-conductr-bundle-lib"))
  .dependsOn(conductRBundleLib)
  .dependsOn(scalaCommon)
  .dependsOn(testLib % "test->compile")

lazy val scalaConductRClientLib = project
  .in(file("scala-conductr-client-lib"))
  .dependsOn(scalaCommon)
  .dependsOn(testLib % "test->compile")


// Akka 2.3
lazy val akka23Common = project
  .in(file("akka23-common"))
  .dependsOn(scalaCommon)

lazy val akka23ConductRBundleLib = project
  .in(file("akka23-conductr-bundle-lib"))
  .dependsOn(scalaConductRBundleLib)
  .dependsOn(akka23Common)
  .dependsOn(testLib % "test->compile")

lazy val akka23ConductRClientLib = project
  .in(file("akka23-conductr-client-lib"))
  .dependsOn(scalaConductRClientLib)
  .dependsOn(akka23Common)
  .dependsOn(testLib % "test->compile")


// Play 2.3
lazy val play23Common = project
  .in(file("play23-common"))
  .dependsOn(scalaCommon)

lazy val play23ConductRBundleLib = project
  .in(file("play23-conductr-bundle-lib"))
  .dependsOn(akka23ConductRBundleLib)
  .dependsOn(play23Common)
  .dependsOn(testLib % "test->compile")

lazy val play23ConductRClientLib = project
  .in(file("play23-conductr-client-lib"))
  .dependsOn(scalaConductRClientLib)
  .dependsOn(play23Common)
  .dependsOn(testLib % "test->compile")


// Play 2.4
lazy val play24Common = project
  .in(file("play24-common"))
  .dependsOn(scalaCommon)

lazy val play24ConductRBundleLib = project
  .in(file("play24-conductr-bundle-lib"))
  .dependsOn(akka23ConductRBundleLib)
  .dependsOn(play24Common)
  .dependsOn(testLib % "test->compile")

lazy val play24ConductRClientLib = project
  .in(file("play24-conductr-client-lib"))
  .dependsOn(scalaConductRClientLib)
  .dependsOn(play24Common)
  .dependsOn(testLib % "test->compile")


// Test library
lazy val testLib = project
  .in(file("test-lib"))
  
name := "root"

publishArtifact := false
