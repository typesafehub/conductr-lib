import Tests._

name := "java-conductr-bundle-lib"

libraryDependencies ++= List(
  Library.java8Compat % "test",
  Library.scalaTest   % "test"
)

javacOptions in compile ++= List("-encoding", "UTF-8", "-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-Xlint:deprecation")
javacOptions in doc ++= List("-encoding", "UTF-8", "-source", "1.8")

unmanagedSourceDirectories in Compile := List((javaSource in Compile).value)

autoScalaLibrary := false
crossPaths := false
crossScalaVersions := List(crossScalaVersions.value.head) // Requires building just the once

fork in Test := true

def groupByFirst(tests: Seq[TestDefinition]) =
  tests
    .groupBy(t => if (t.name.endsWith("SpecWithEnv")) "WithEnv" else "WithoutEnv")
    .map {
      case ("WithEnv", t) =>
        new Group("WithEnv", t, SubProcess(ForkOptions(envVars = Map(
          "BUNDLE_ID" -> "0BADF00DDEADBEEF",
          "CONDUCTR_STATUS" -> "http://127.0.0.1:20007",
          "SERVICE_LOCATOR" -> "http://127.0.0.1:20008/services"))))
      case ("WithoutEnv", t) =>
        new Group("WithoutEnv", t, SubProcess(ForkOptions()))
    }
    .toSeq

testGrouping in Test <<= (definedTests in Test).map(groupByFirst)
