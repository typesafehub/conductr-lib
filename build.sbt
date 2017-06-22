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
    akka24Common,
    akka24ConductRBundleLib,
    akka24ConductRClientLib,
    akka24TestLib,
    akka25Common,
    akka25ConductRBundleLib,
    akka25ConductRClientLib,
    akka25TestLib,
    play25Common,
    play25ConductRBundleLib,
    play25ConductRClientLib,
    play26Common,
    play26ConductRBundleLib,
    play26ConductRClientLib,
    lagom13JavaConductRBundleLib,
    lagom13ScalaConductRBundleLib)
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


// Akka 2.4
lazy val akka25Common = project
  .in(file("akka25-common"))
  .dependsOn(scalaCommon)
  .enablePlugins(CrossPerProjectPlugin)

lazy val akka25ConductRBundleLib = project
  .in(file("akka25-conductr-bundle-lib"))
  .dependsOn(scalaConductRBundleLib)
  .dependsOn(akka25Common)
  .dependsOn(akka25TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

lazy val akka25ConductRClientLib = project
  .in(file("akka25-conductr-client-lib"))
  .dependsOn(scalaConductRClientLib)
  .dependsOn(akka25Common)
  .dependsOn(akka25TestLib % "test->compile")
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


// Play 2.6
lazy val play26Common = project
  .in(file("play26-common"))
  .dependsOn(javaCommon)
  .dependsOn(scalaCommon)
  .enablePlugins(CrossPerProjectPlugin)

lazy val play26ConductRBundleLib = project
  .in(file("play26-conductr-bundle-lib"))
  .dependsOn(akka25ConductRBundleLib)
  .dependsOn(play26Common)
  .dependsOn(javaTestLib % "test->compile")
  .dependsOn(akka25TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

lazy val play26ConductRClientLib = project
  .in(file("play26-conductr-client-lib"))
  .dependsOn(scalaConductRClientLib)
  .dependsOn(play26Common)
  .dependsOn(javaTestLib % "test->compile")
  .dependsOn(akka25TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)


// Lagom version 1
lazy val lagom13JavaConductRBundleLib = project
  .in(file("lagom13-java-conductr-bundle-lib"))
  .dependsOn(play25ConductRBundleLib)
  .dependsOn(javaTestLib % "test->compile")
  .dependsOn(akka24TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)

lazy val lagom13ScalaConductRBundleLib = project
  .in(file("lagom13-scala-conductr-bundle-lib"))
  .dependsOn(play25ConductRBundleLib)
  .dependsOn(javaTestLib % "test->compile")
  .dependsOn(akka24TestLib % "test->compile")
  .enablePlugins(CrossPerProjectPlugin)


// Test libraries
lazy val akka24TestLib = project
  .in(file("akka24-test-lib"))
  .enablePlugins(CrossPerProjectPlugin)

lazy val akka25TestLib = project
  .in(file("akka25-test-lib"))
  .enablePlugins(CrossPerProjectPlugin)

lazy val javaTestLib = project
  .in(file("java-test-lib"))
  .enablePlugins(CrossPerProjectPlugin)


name := "conductr-lib"
