import Tests._

name := "lagom14-scala-conductr-bundle-lib"

libraryDependencies ++= List(
  Library.lagom14ClientScaladsl,
  Library.lagom14ServerScaladsl % "test",
  Library.akka25Testkit         % "test",
  Library.play26Test            % "test",
  Library.scalaTest             % "test"
)

crossScalaVersions := crossScalaVersions.value.filterNot(_.startsWith("2.12"))

fork in Test := true

def groupByFirst(tests: Seq[TestDefinition]) =
  tests
    .groupBy(t => t.name.drop(t.name.indexOf("WithEnv")))
    .map {
      case ("WithEnv", t) =>
        new Group("WithEnv", t, SubProcess(ForkOptions(envVars = Map(
          "BUNDLE_ID" -> "0BADF00DDEADBEEF",
          "BUNDLE_SYSTEM" -> "somesys",
          "BUNDLE_SYSTEM_VERSION" -> "v1",
          "SERVICE_LOCATOR" -> "http://127.0.0.1:30008/services",
          "BUNDLE_NAME" -> "my-project"
        ))))
    }.toSeq

testGrouping in Test := { (definedTests in Test).map(groupByFirst).value }
