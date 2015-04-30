import Tests._

name := "scala-conductr-bundle-lib"

libraryDependencies ++= List(
  Library.akka23HttpTestkit % "test",
  Library.akka23Testkit     % "test",
  Library.scalaTest         % "test"
)

fork in Test := true

def groupByFirst(tests: Seq[TestDefinition]) =
  tests
    .groupBy(t => if (t.name.endsWith("SpecWithEnv")) "WithEnv" else "WithoutEnv")
    .map {
      case ("WithEnv", t) =>
        new Group("WithEnv", t, SubProcess(ForkOptions(envVars = Map(
          "BUNDLE_ID" -> "0BADF00DDEADBEEF",
          "CONDUCTR_STATUS" -> "http://127.0.0.1:50007",
          "SERVICE_LOCATOR" -> "http://127.0.0.1:50008/services"))))
      case ("WithoutEnv", t) =>
        new Group("WithoutEnv", t, SubProcess(ForkOptions()))
    }
    .toSeq

testGrouping in Test <<= (definedTests in Test).map(groupByFirst)
