lazy val root = project
  .in(file("."))
  .aggregate(
    common,
    conductRBundleLib,
    javaCommon,
    javaConductRBundleLib,
    scalaCommon,
    scalaConductRBundleLib,
    scalaConductRClientLib,
    akka23Common,
    akka23ConductRBundleLib,
    akka23ConductRClientLib,
    akka24Common,
    akka24ConductRBundleLib,
    akka24ConductRClientLib,
    play23Common,
    play23ConductRBundleLib,
    play23ConductRClientLib,
    play24Common,
    play24ConductRBundleLib,
    play24ConductRClientLib,
    play25Common,
    play25ConductRBundleLib,
    play25ConductRClientLib)


// Base
lazy val common = project
  .in(file("common"))
  .dependsOn(scalaTestLib % "test->compile")

lazy val conductRBundleLib = project
  .in(file("conductr-bundle-lib"))
  .dependsOn(common)
  .dependsOn(scalaTestLib % "test->compile")

// Java
lazy val javaCommon = project
  .in(file("java-common"))
  .dependsOn(common)

lazy val javaConductRBundleLib = project
  .in(file("java-conductr-bundle-lib"))
  .dependsOn(conductRBundleLib)
  .dependsOn(javaCommon)
  .dependsOn(javaTestLib % "test->compile")

// Scala
lazy val scalaCommon = project
  .in(file("scala-common"))
  .dependsOn(common)

lazy val scalaConductRBundleLib = project
  .in(file("scala-conductr-bundle-lib"))
  .dependsOn(conductRBundleLib)
  .dependsOn(scalaCommon)
  .dependsOn(scalaTestLib % "test->compile")

lazy val scalaConductRClientLib = project
  .in(file("scala-conductr-client-lib"))
  .dependsOn(scalaCommon)
  .dependsOn(scalaTestLib % "test->compile")


// Akka 2.3
lazy val akka23Common = project
  .in(file("akka23-common"))
  .dependsOn(scalaCommon)

lazy val akka23ConductRBundleLib = project
  .in(file("akka23-conductr-bundle-lib"))
  .dependsOn(scalaConductRBundleLib)
  .dependsOn(akka23Common)
  .dependsOn(scalaTestLib % "test->compile")

lazy val akka23ConductRClientLib = project
  .in(file("akka23-conductr-client-lib"))
  .dependsOn(scalaConductRClientLib)
  .dependsOn(akka23Common)
  .dependsOn(scalaTestLib % "test->compile")

// Akka 2.4
lazy val akka24Common = project
  .in(file("akka24-common"))
  .dependsOn(scalaCommon)

lazy val akka24ConductRBundleLib = project
  .in(file("akka24-conductr-bundle-lib"))
  .dependsOn(scalaConductRBundleLib)
  .dependsOn(akka24Common)
  .dependsOn(scalaTestLib % "test->compile")

lazy val akka24ConductRClientLib = project
  .in(file("akka24-conductr-client-lib"))
  .dependsOn(scalaConductRClientLib)
  .dependsOn(akka24Common)
  .dependsOn(scalaTestLib % "test->compile")  

// Play 2.3
lazy val play23Common = project
  .in(file("play23-common"))
  .dependsOn(scalaCommon)

lazy val play23ConductRBundleLib = project
  .in(file("play23-conductr-bundle-lib"))
  .dependsOn(akka23ConductRBundleLib)
  .dependsOn(play23Common)
  .dependsOn(scalaTestLib % "test->compile")

lazy val play23ConductRClientLib = project
  .in(file("play23-conductr-client-lib"))
  .dependsOn(scalaConductRClientLib)
  .dependsOn(play23Common)
  .dependsOn(scalaTestLib % "test->compile")


// Play 2.4
lazy val play24Common = project
  .in(file("play24-common"))
  .dependsOn(scalaCommon)

lazy val play24ConductRBundleLib = project
  .in(file("play24-conductr-bundle-lib"))
  .dependsOn(akka23ConductRBundleLib)
  .dependsOn(play24Common)
  .dependsOn(scalaTestLib % "test->compile")

lazy val play24ConductRClientLib = project
  .in(file("play24-conductr-client-lib"))
  .dependsOn(scalaConductRClientLib)
  .dependsOn(play24Common)
  .dependsOn(scalaTestLib % "test->compile")

// Play 2.5
lazy val play25Common = project
  .in(file("play25-common"))
  .dependsOn(scalaCommon)

lazy val play25ConductRBundleLib = project
  .in(file("play25-conductr-bundle-lib"))
  .dependsOn(akka24ConductRBundleLib)
  .dependsOn(play25Common)
  .dependsOn(scalaTestLib % "test->compile")

lazy val play25ConductRClientLib = project
  .in(file("play25-conductr-client-lib"))
  .dependsOn(scalaConductRClientLib)
  .dependsOn(play25Common)
  .dependsOn(scalaTestLib % "test->compile")  

// Test libraries
lazy val scalaTestLib = project
  .in(file("scala-test-lib"))

lazy val javaTestLib = project
  .in(file("java-test-lib"))
  .dependsOn(scalaTestLib)

name := "root"
