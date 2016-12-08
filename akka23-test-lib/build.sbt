name := "akka23-test-lib"

libraryDependencies ++= List(
  Library.akka23Testkit,
  Library.akka23HttpTestkit,
  Library.junit,
  Library.scalaTest
)

crossScalaVersions := crossScalaVersions.value.filterNot(_.startsWith("2.12"))
