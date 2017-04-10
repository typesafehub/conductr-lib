lazy val root = project
  .in(file("."))
  .aggregate(
    common,
    conductRBundleLib,
    javaCommon,
    javaConductRBundleLib,
    javaTestLib,
    scalaCommon,
    scalaConductRBundleLib,
    scalaConductRClientLib,
    akka23Common,
    akka23ConductRBundleLib,
    akka23TestLib,
    akka24Common,
    akka24ConductRBundleLib,
    akka24ConductRClientLib,
    akka24TestLib,
    play23Common,
    play23ConductRBundleLib,
    play23ConductRClientLib,
    play24Common,
    play24ConductRBundleLib,
    play24ConductRClientLib,
    play25Common,
    play25ConductRBundleLib,
    play25ConductRClientLib,
    lagom1JavaConductRBundleLib,
    lagom1ScalaConductRBundleLib)
  .enablePlugins(CrossPerProjectPlugin)

// When executing tests the projects are running sequentially.
// If the tests of each project run sequentially or in parallel
// is defined in the `build.sbt` of the individual project itself
concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)


// Base
lazy val common = project
  .in(file("common"))
  .dependsOn(akka24TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

lazy val conductRBundleLib = project
  .in(file("conductr-bundle-lib"))
  .dependsOn(common)
  .dependsOn(akka24TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

// Java
lazy val javaCommon = project
  .in(file("java-common"))
  .dependsOn(common)
  .enablePlugins(CrossPerProjectPlugin)

lazy val javaConductRBundleLib = project
  .in(file("java-conductr-bundle-lib"))
  .dependsOn(conductRBundleLib)
  .dependsOn(javaCommon)
  .dependsOn(javaTestLib % "test->compile")
  .dependsOn(akka24TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

// Scala
lazy val scalaCommon = project
  .in(file("scala-common"))
  .dependsOn(common)
  .enablePlugins(CrossPerProjectPlugin)

lazy val scalaConductRBundleLib = project
  .in(file("scala-conductr-bundle-lib"))
  .dependsOn(conductRBundleLib)
  .dependsOn(scalaCommon)
  .dependsOn(akka24TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

lazy val scalaConductRClientLib = project
  .in(file("scala-conductr-client-lib"))
  .dependsOn(scalaCommon)
  .dependsOn(akka24TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

// Akka 2.3
lazy val akka23Common = project
  .in(file("akka23-common"))
  .dependsOn(scalaCommon)
  .enablePlugins(CrossPerProjectPlugin)

lazy val akka23ConductRBundleLib = project
  .in(file("akka23-conductr-bundle-lib"))
  .dependsOn(scalaConductRBundleLib)
  .dependsOn(akka23Common)
  .dependsOn(akka23TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

// Akka 2.4
lazy val akka24Common = project
  .in(file("akka24-common"))
  .dependsOn(scalaCommon)
  .enablePlugins(CrossPerProjectPlugin)

lazy val akka24ConductRBundleLib = project
  .in(file("akka24-conductr-bundle-lib"))
  .dependsOn(scalaConductRBundleLib)
  .dependsOn(akka24Common)
  .dependsOn(akka24TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

lazy val akka24ConductRClientLib = project
  .in(file("akka24-conductr-client-lib"))
  .dependsOn(scalaConductRClientLib)
  .dependsOn(akka24Common)
  .dependsOn(akka24TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

// Play 2.3
lazy val play23Common = project
  .in(file("play23-common"))
  .dependsOn(scalaCommon)
  .enablePlugins(CrossPerProjectPlugin)

lazy val play23ConductRBundleLib = project
  .in(file("play23-conductr-bundle-lib"))
  .dependsOn(akka23ConductRBundleLib)
  .dependsOn(play23Common)
  .dependsOn(akka23TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

lazy val play23ConductRClientLib = project
  .in(file("play23-conductr-client-lib"))
  .dependsOn(scalaConductRClientLib)
  .dependsOn(play23Common)
  .dependsOn(akka23TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)


// Play 2.4
lazy val play24Common = project
  .in(file("play24-common"))
  .dependsOn(scalaCommon)
  .enablePlugins(CrossPerProjectPlugin)

lazy val play24ConductRBundleLib = project
  .in(file("play24-conductr-bundle-lib"))
  .dependsOn(akka23ConductRBundleLib)
  .dependsOn(play24Common)
  .dependsOn(akka23TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

lazy val play24ConductRClientLib = project
  .in(file("play24-conductr-client-lib"))
  .dependsOn(scalaConductRClientLib)
  .dependsOn(play24Common)
  .dependsOn(akka23TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

// Play 2.5
lazy val play25Common = project
  .in(file("play25-common"))
  .dependsOn(javaCommon)
  .dependsOn(scalaCommon)
  .enablePlugins(CrossPerProjectPlugin)

lazy val play25ConductRBundleLib = project
  .in(file("play25-conductr-bundle-lib"))
  .dependsOn(akka24ConductRBundleLib)
  .dependsOn(play25Common)
  .dependsOn(javaTestLib % "test->compile")
  .dependsOn(akka24TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

lazy val play25ConductRClientLib = project
  .in(file("play25-conductr-client-lib"))
  .dependsOn(scalaConductRClientLib)
  .dependsOn(play25Common)
  .dependsOn(javaTestLib % "test->compile")
  .dependsOn(akka24TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

// Lagom version 1
lazy val lagom1JavaConductRBundleLib = project
  .in(file("lagom1-java-conductr-bundle-lib"))
  .dependsOn(play25ConductRBundleLib)
  .dependsOn(javaTestLib % "test->compile")
  .dependsOn(akka24TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

lazy val lagom1ScalaConductRBundleLib = project
  .in(file("lagom1-scala-conductr-bundle-lib"))
  .dependsOn(play25ConductRBundleLib)
  .dependsOn(javaTestLib % "test->compile")
  .dependsOn(akka24TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

// Test libraries
lazy val akka23TestLib = project
  .in(file("akka23-test-lib"))
  .enablePlugins(CrossPerProjectPlugin)

lazy val akka24TestLib = project
  .in(file("akka24-test-lib"))
  .enablePlugins(CrossPerProjectPlugin)

lazy val javaTestLib = project
  .in(file("java-test-lib"))
  .enablePlugins(CrossPerProjectPlugin)


name := "root"
