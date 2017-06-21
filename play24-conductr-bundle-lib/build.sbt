import Tests._

name := "play24-conductr-bundle-lib"

libraryDependencies ++= List(
  // Adding play24Ws here is necessary because play24-common is adding the library as 'provided'
  Library.play24Ws      % "provided",
  Library.akka23Testkit % "test",
  Library.play24Test    % "test",
  Library.scalaTest     % "test"
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
          "CONDUCTR_STATUS" -> "http://127.0.0.1:30007",
          "SERVICE_LOCATOR" -> "http://127.0.0.1:30008/services",
          "WEB_BIND_IP" -> "127.0.0.1",
          "WEB_BIND_PORT" -> "9024"
        ))))
      case (x, t) =>
        new Group("WithoutEnv", t, SubProcess(ForkOptions()))
    }.toSeq

testGrouping in Test := { (definedTests in Test).map(groupByFirst).value }
